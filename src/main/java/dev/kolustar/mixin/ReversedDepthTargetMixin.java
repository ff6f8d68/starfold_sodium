package dev.kolustar.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.kolustar.render.ReversedDepth;

import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Ahead of Veil, whose clear replacement writes a constant far depth of 1.0 straight to the texture.
@Mixin(value = RenderTarget.class, priority = 500)
public abstract class ReversedDepthTargetMixin {
    @Shadow public boolean useDepth;
    @Shadow @org.spongepowered.asm.mixin.Final private float[] clearChannels;
    @Shadow public abstract void bindWrite(boolean viewport);
    @Shadow public abstract void unbindWrite();

    @Inject(method = "clear(Z)V", at = @At("HEAD"), cancellable = true)
    private void kolustar$clear(boolean error, CallbackInfo ci) {
        if (!ReversedDepth.ACTIVE) return;
        ci.cancel();
        bindWrite(true);
        com.mojang.blaze3d.platform.GlStateManager._clearColor(clearChannels[0], clearChannels[1], clearChannels[2], clearChannels[3]);
        int mask = GL30.GL_COLOR_BUFFER_BIT;
        if (useDepth) {
            com.mojang.blaze3d.platform.GlStateManager._clearDepth(1.0);
            mask |= GL30.GL_DEPTH_BUFFER_BIT;
        }
        com.mojang.blaze3d.platform.GlStateManager._clear(mask, error);
        unbindWrite();
    }

    @Inject(method = "createBuffers(IIZ)V", at = @At("HEAD"))
    private void kolustar$depthInit(int width, int height, boolean error, CallbackInfo ci) {
        ReversedDepth.initialize();
    }

    @ModifyArg(
            method = "createBuffers(IIZ)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"),
            index = 2)
    private int kolustar$depthFormat(int format) {
        return ReversedDepth.ACTIVE && format == GL30.GL_DEPTH_COMPONENT
                ? GL30.GL_DEPTH_COMPONENT32F : format;
    }
}
