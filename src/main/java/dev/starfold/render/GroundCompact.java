package dev.starfold.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.util.*;

/** Twelve-byte chart vertices; the shared palette holds sprite bounds, tint and albedo. */
final class GroundCompact {
    static final String NAME = "starfold:ground_compact";
    static final VertexFormat FORMAT = VertexFormat.builder()
            .add("PackedPosition", VertexFormatElement.register(VertexFormatElement.findNextId(), 0,
                    VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.POSITION, 3))
            .add("Packed", VertexFormatElement.register(VertexFormatElement.findNextId(), 0,
                    VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 3)).build();
    /** Palette entry; compared field by field so a lookup never allocates a probe object. */
    private static final class Material {
        final float u, v, alpha;
        final int bounds, tint, normal;

        Material(float u, float v, int bounds, int tint, int normal, float alpha) {
            this.u = u; this.v = v; this.bounds = bounds; this.tint = tint;
            this.normal = normal; this.alpha = alpha;
        }

        boolean same(float u, float v, int bounds, int tint, int normal, float alpha) {
            return this.bounds == bounds && this.tint == tint && this.normal == normal
                    && Float.compare(this.u, u) == 0 && Float.compare(this.v, v) == 0
                    && Float.compare(this.alpha, alpha) == 0;
        }
    }
    private static final List<Material> materials = new ArrayList<>();
    // Open-addressed palette index keyed on the material fields: a LOD tile interns thousands of
    // vertices, and this is the only per-vertex lookup on the render thread.
    private static Material[] slots = new Material[256];
    private static int[] slotIds = new int[256];
    private static int mask = 255, used;
    private static ShaderInstance shader;
    private static int texture, uploaded;
    private static long paletteBytes;

    static void initialize(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterShadersEvent event) -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse(NAME), FORMAT), s -> {
                    shader = s;
                    clear();
                });
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }

    static void clear() {
        Arrays.fill(slots, null);
        used = 0;
        materials.clear();
        uploaded = 0;
        paletteBytes = 0;
        if (texture != 0) GL11.glDeleteTextures(texture);
        texture = 0;
    }

    static long gpuBytes() { return paletteBytes; }

    private static int hash(float u, float v, int bounds, int tint, int normal, float alpha) {
        int h = Float.floatToRawIntBits(u) * 31 + Float.floatToRawIntBits(v);
        h = h * 31 + Float.floatToRawIntBits(alpha);
        h = h * 31 + bounds;
        h = h * 31 + tint;
        h = h * 31 + normal;
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return h;
    }

    private static void grow() {
        var oldSlots = slots;
        var oldIds = slotIds;
        slots = new Material[oldSlots.length * 2];
        slotIds = new int[slots.length];
        mask = slots.length - 1;
        for (int i = 0; i < oldSlots.length; i++) {
            var entry = oldSlots[i];
            if (entry == null) continue;
            int h = hash(entry.u, entry.v, entry.bounds, entry.tint, entry.normal, entry.alpha) & mask;
            while (slots[h] != null) h = (h + 1) & mask;
            slots[h] = entry;
            slotIds[h] = oldIds[i];
        }
    }

    /** Palette id for one material, allocating only the first time that material is seen. */
    static int index(float u, float v, int bounds, int tint, int normal, float alpha) {
        int h = hash(u, v, bounds, tint, normal, alpha) & mask;
        while (true) {
            var entry = slots[h];
            if (entry == null) break;
            if (entry.same(u, v, bounds, tint, normal, alpha)) return slotIds[h];
            h = (h + 1) & mask;
        }
        var entry = new Material(u, v, bounds, tint, normal, alpha);
        materials.add(entry);
        int id = materials.size() - 1;
        slots[h] = entry;
        slotIds[h] = id;
        if (++used * 2 > slots.length) grow();
        return id;
    }

    static ShaderInstance shader() {
        if (uploaded != materials.size()) {
            if (texture == 0) texture = GL11.glGenTextures();
            int height = Integer.highestOneBit(Math.max(1, (materials.size() * 3 + 255) / 256) - 1) * 2;
            height = Math.max(1, height);
            var data = MemoryUtil.memAllocFloat(256 * height * 4);
            try {
                for (var m : materials) {
                    data.put(m.u).put(m.v).put((m.bounds & 65535) / 65535f).put((m.bounds >>> 16) / 65535f);
                    data.put((m.tint & 255) / 255f).put((m.tint >> 8 & 255) / 255f).put((m.tint >> 16 & 255) / 255f).put(1);
                    data.put((byte)m.normal / 127f).put((byte)(m.normal >> 8) / 127f).put((byte)(m.normal >> 16) / 127f).put(m.alpha);
                }
                data.position(0);
                int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                // Vanilla sub-image uploads (the pause menu among them) leave skip rows set; read from the start.
                int[] unpack = {GL11.GL_UNPACK_ALIGNMENT, GL11.GL_UNPACK_ROW_LENGTH, GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_SKIP_PIXELS};
                int[] saved = new int[unpack.length];
                for (int i = 0; i < unpack.length; i++) {
                    saved[i] = GL11.glGetInteger(unpack[i]);
                    GL11.glPixelStorei(unpack[i], i == 0 ? 4 : 0);
                }
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 256, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, data);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
                for (int i = 0; i < unpack.length; i++) GL11.glPixelStorei(unpack[i], saved[i]);
                paletteBytes = (long)256 * height * 16;
                uploaded = materials.size();
            } finally { MemoryUtil.memFree(data); }
        }
        shader.setSampler("CPMaterials", texture);
        return shader;
    }

    static MeshData pack(MeshData source, ByteBufferBuilder memory, net.minecraft.client.renderer.RenderType layer, int face, long lodKey) {
        var data = source.vertexBuffer();
        int count = source.drawState().vertexCount();
        long target = memory.reserve(count * 12);
        int light=0;
        for (int i = 0; i < count; i++) {
            if (i%4==0) light=GroundRenderer.surfaceLight(face,lightPosition(face,data,i*32));
            int p = i * 32;
            int id = index(data.getFloat(p + 16), data.getFloat(p + 20), data.getInt(p + 24),
                    data.getInt(p + 12) & 0xffffff, data.getInt(p + 28) & 0xffffff,
                    layer == net.minecraft.client.renderer.RenderType.translucent() ? .7f : -1f);
            put(target + i * 12L, data.getFloat(p), data.getFloat(p + 4), data.getFloat(p + 8),
                    id, Byte.toUnsignedInt(data.get(p + 15)), light);
            if (lodKey >= 0) {
                MemoryUtil.memPutShort(target + i * 12L, (short)tilePosition(data.getFloat(p), (int)(lodKey % 128)));
                MemoryUtil.memPutShort(target + i * 12L + 4, (short)tilePosition(data.getFloat(p + 8), (int)(lodKey / 128 % 128)));
            }
        }
        source.close();
        return new MeshData(memory.build(), new MeshData.DrawState(FORMAT, count, count / 4 * 6,
                VertexFormat.Mode.QUADS, VertexFormat.IndexType.least(count)));
    }

    static BlockPos lightPosition(int face, java.nio.ByteBuffer vertices, int start) {
        double x=0,y=0,z=0,top=-Double.MAX_VALUE;
        for(int i=0;i<4;i++) {
            int p=start+i*32;
            x+=vertices.getFloat(p);y+=vertices.getFloat(p+4);z+=vertices.getFloat(p+8);
            top=Math.max(top,vertices.getFloat(p+4));
        }
        // (b-a) x (c-a) in primitives; the quad corner Points were four allocations per face.
        double ax=vertices.getFloat(start), ay=vertices.getFloat(start+4), az=vertices.getFloat(start+8);
        double ux=vertices.getFloat(start+32)-ax, uy=vertices.getFloat(start+36)-ay, uz=vertices.getFloat(start+40)-az;
        double vx=vertices.getFloat(start+64)-ax, vy=vertices.getFloat(start+68)-ay, vz=vertices.getFloat(start+72)-az;
        double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
        // Closure walls extend underground; light their exposed rim, not their buried centre.
        return BlockPos.containing(face*4096+x/4+Math.signum(nx)*.01,
                ny==0 ? Math.ceil(128+top) : ny>0 ? Math.ceil(128+y/4) : 128+y/4-.01,
                z/4+Math.signum(nz)*.01);
    }

    static void put(long p, double x, double y, double z, int material, int tag, int light) {
        MemoryUtil.memPutShort(p, (short)Math.round(x * 16));
        MemoryUtil.memPutShort(p + 2, (short)Math.round((y + 2048) * 16));
        MemoryUtil.memPutShort(p + 4, (short)Math.round(z * 16));
        MemoryUtil.memPutShort(p + 6, (short)material);
        MemoryUtil.memPutShort(p + 8, (short)(tag | (material >>> 16) << 8));
        MemoryUtil.memPutShort(p + 10, (short)light);
    }

    // Seven tile bits plus nine local sixteenth-block bits, including the exact +16 edge.
    static int tilePosition(double position, int tile) {
        return tile << 9 | (int)Math.round((position - tile * 16) * 16);
    }

    /** Decode before the ordinary ground wrapper; projection, ownership and mip sampling stay shared. */
    static String unpack(String source) {
        return source.replace("uniform float CPNativeLodPass;", "")
                .replace("// CP_NATIVE_LOD_TILE", "if(CPNativeLodPass>0.5){vec2 tile=floor(PackedPosition.xz/512.0)*16.0;cpNativeSourcePoint.xz=clamp(cpNativeSourcePoint.xz,tile+vec2(0.001),tile+vec2(15.999));}")
                .replace("if(CPNativeActive>0.5&&CPNativeCover>0.5&&cpNativeFarTag(round(Color.a*255.0)))cpNativeLightUV2=ivec2(240);", "")
                .replace("cpNativeTextureAlpha=1.0;",  "cpNativeTextureAlpha=cpMaterial((int(Packed.x)+int(Packed.y>>8u)*65536)*3+2).w;")
                .replace("in vec3 Position;", "in vec3 PackedPosition;\nvec3 Position;")
                .replace("in vec4 Color;", "in uvec3 Packed;\nvec4 Color;")
                .replace("in vec2 UV0;", "vec2 UV0;")
                .replace("in ivec2 UV2;", "ivec2 UV2;")
                .replace("in vec3 Normal;", "vec3 Normal;")
                .replace("vec4 cpNativeModelView;", """
                        uniform sampler2D CPMaterials;
                        uniform float CPNativeLodPass;
                        vec4 cpMaterial(int i){return texelFetch(CPMaterials,ivec2(i%256,i/256),0);}
                        vec4 cpNativeModelView;
                        """)
                .replace("cpNativePackedBounds=UV2;cpNativeLightUV2=UV2;", """
                        Position=PackedPosition/16.0-vec3(0,2048,0);
                        if(CPNativeLodPass>0.5)Position.xz=floor(PackedPosition.xz/512.0)*16.0+mod(PackedPosition.xz,512.0)/16.0;
                        int material=(int(Packed.x)+int(Packed.y>>8u)*65536)*3;
                        vec4 bounds=cpMaterial(material);
                        Color=vec4(cpMaterial(material+1).rgb,float(Packed.y&255u)/255.0);
                        UV0=bounds.xy;UV2=ivec2(round(bounds.zw*65535.0));Normal=cpMaterial(material+2).xyz;
                        cpNativePackedBounds=UV2;
                        cpNativeLightUV2=ivec2(int(Packed.z)&255,int(Packed.z)>>8);
                        """);
    }
    private GroundCompact() {}
}
