package dev.kolustar.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Additive half-resolution bloom for Kolustar scenes: bright extraction, one separable Gaussian
 * blur and a composite back onto the main framebuffer. Emissive suns, glowing cores and engines
 * bleed into their surroundings without the CPU sampling anything.
 *
 * <p>Configurable with {@code -Dkolustar.bloom=false}, {@code -Dkolustar.bloom.threshold=0.72},
 * {@code -Dkolustar.bloom.knee=0.35} and {@code -Dkolustar.bloom.strength=0.9}. Bloom is skipped
 * in dimensions Kolustar does not draw, so other worlds keep vanilla's look.
 */
public final class BloomPost {
    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("kolustar.bloom", "true"));
    private static final float THRESHOLD = property("kolustar.bloom.threshold", 0.72f);
    private static final float KNEE = property("kolustar.bloom.knee", 0.35f);
    private static final float STRENGTH = property("kolustar.bloom.strength", 0.9f);

    private static ShaderInstance extract, blur, composite;
    private static final int[] framebuffers = {-1, -1};
    private static final int[] textures = {-1, -1};
    private static int width = -1, height = -1;
    private static boolean broken;

    private BloomPost() {}

    public static void initialize(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(
                (RegisterShadersEvent event) -> {
                    try {
                        event.registerShader(
                                shader(event, "extract"), s -> extract = s);
                        event.registerShader(shader(event, "blur"), s -> blur = s);
                        event.registerShader(
                                shader(event, "composite"), s -> composite = s);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.RenderLevelStageEvent event) -> {
                    if (event.getStage()
                            == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage
                                    .AFTER_LEVEL) render();
                });
    }

    private static ShaderInstance shader(RegisterShadersEvent event, String pass)
            throws IOException {
        return new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.parse("kolustar:kolustar_bloom_" + pass),
                DefaultVertexFormat.POSITION_TEX);
    }

    private static float property(String name, float fallback) {
        try {
            return Float.parseFloat(System.getProperty(name, Float.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void render() {
        if (!ENABLED || broken || extract == null || blur == null || composite == null) return;
        if (!GroundClient.active() && !dev.kolustar.space.SpaceClient.active()) return;
        var main = Minecraft.getInstance().getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        int w = Math.max(1, main.width / 2), h = Math.max(1, main.height / 2);
        if (!ensure(w, h)) return;

        var state = new RenderPassState(true, true, false);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int unit = GlStateManager._getActiveTexture();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);

            // Bright pass: everything above the knee becomes the bloom source.
            set(extract, "Threshold", THRESHOLD);
            set(extract, "Knee", Math.max(KNEE, 1e-4f));
            pass(extract, framebuffers[0], main.getColorTextureId(), w, h);

            // Separable Gaussian: horizontal into the scratch target, vertical back.
            set(blur, "TexelSize", 1f / w, 1f / h);
            set(blur, "Direction", 1f, 0f);
            pass(blur, framebuffers[1], textures[0], w, h);
            set(blur, "Direction", 0f, 1f);
            pass(blur, framebuffers[0], textures[1], w, h);

            // Additive composite over the already rendered level.
            main.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ZERO,
                    GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(() -> composite);
            composite.setSampler("Sampler0", textures[0]);
            set(composite, "Strength", STRENGTH);
            quad();
            composite.clear();
        } finally {
            try {
                extract.clear();
                blur.clear();
                composite.clear();
            } finally {
                main.bindWrite(true);
                GlStateManager._activeTexture(unit);
                state.close();
                ProgramManager.glUseProgram(program);
            }
        }
    }

    /** Runs one full-screen pass into {@code target}, sampling {@code texture}. */
    private static void pass(ShaderInstance shader, int target, int texture, int w, int h) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target);
        GlStateManager._viewport(0, 0, w, h);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("Sampler0", texture);
        quad();
        shader.clear();
    }

    private static void quad() {
        var builder =
                Tesselator.getInstance()
                        .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1f, -1f, 0f).setUv(0f, 0f);
        builder.addVertex(1f, -1f, 0f).setUv(1f, 0f);
        builder.addVertex(1f, 1f, 0f).setUv(1f, 1f);
        builder.addVertex(-1f, 1f, 0f).setUv(0f, 1f);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void set(ShaderInstance shader, String name, float... values) {
        var uniform = shader.getUniform(name);
        if (uniform == null) return;
        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> throw new IllegalArgumentException(name);
        }
    }

    /** Half-resolution colour targets, recreated on window resize. */
    private static boolean ensure(int w, int h) {
        if (w == width && h == height && framebuffers[0] != -1) return true;
        release();
        for (int i = 0; i < 2; i++) {
            textures[i] = GlStateManager._genTexture();
            GlStateManager._bindTexture(textures[i]);
            GlStateManager._texImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    w,
                    h,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    null);
            GlStateManager._texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            framebuffers[i] = GlStateManager.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffers[i]);
            GlStateManager._glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D,
                    textures[i],
                    0);
            if (GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
                    != GL30.GL_FRAMEBUFFER_COMPLETE) {
                broken = true;
                release();
                return false;
            }
        }
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GlStateManager._bindTexture(0);
        width = w;
        height = h;
        return true;
    }

    private static void release() {
        for (int i = 0; i < 2; i++) {
            if (framebuffers[i] != -1) {
                GlStateManager._glDeleteFramebuffers(framebuffers[i]);
                framebuffers[i] = -1;
            }
            if (textures[i] != -1) {
                GlStateManager._deleteTexture(textures[i]);
                textures[i] = -1;
            }
        }
        width = height = -1;
    }

    /** Window resize and world unload release GPU memory rather than leaking targets. */
    public static void clear() {
        release();
        broken = false;
    }
}
