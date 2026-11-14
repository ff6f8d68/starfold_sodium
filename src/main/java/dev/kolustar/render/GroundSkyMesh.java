package dev.kolustar.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.sky.AtmosphereSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Fixed sky topology and directions, uploaded once; the per-frame observer basis is a uniform the
 * vertex shader applies, so a frame rebuilds no vertices.
 */
final class GroundSkyMesh implements AutoCloseable {
    static final int NX = AtmosphereSettings.SKY_LONGITUDES, NY = AtmosphereSettings.SKY_LATITUDES;
    static final int VERTICES = (NX + 1) * (NY + 1);
    private static final double[] EAST = new double[VERTICES], UP = new double[VERTICES],
            NORTH = new double[VERTICES];
    static final int[] INDICES = new int[NX * NY * 6];

    static {
        for (int y = 0; y <= NY; y++)
            for (int x = 0; x <= NX; x++) {
                double a = x * 2 * Math.PI / NX, b = y * Math.PI / NY;
                int i = y * (NX + 1) + x;
                EAST[i] = Math.sin(b) * Math.cos(a);
                UP[i] = Math.cos(b);
                NORTH[i] = Math.sin(b) * Math.sin(a);
            }
        int at = 0;
        for (int y = 0; y < NY; y++)
            for (int x = 0; x < NX; x++) {
                int i = y * (NX + 1) + x;
                INDICES[at++] = i;
                INDICES[at++] = i + 1;
                INDICES[at++] = i + NX + 2;
                INDICES[at++] = i;
                INDICES[at++] = i + NX + 2;
                INDICES[at++] = i + NX + 1;
            }
    }

    /**
     * CPU reference for the basis combination atmosphere_color.vsh now does with SkyEast/SkyUp/
     * SkyNorth; the mesh itself stores the unit-frame directions below.
     */
    static Point direction(int i, Point east, Point up, Point north) {
        return new Point(
                east.x() * EAST[i] + up.x() * UP[i] + north.x() * NORTH[i],
                east.y() * EAST[i] + up.y() * UP[i] + north.y() * NORTH[i],
                east.z() * EAST[i] + up.z() * UP[i] + north.z() * NORTH[i]);
    }

    static void vertex(ByteBuffer out, Point direction, Point color) {
        out.putFloat((float) (direction.x() * 128));
        out.putFloat((float) (direction.y() * 128));
        out.putFloat((float) (direction.z() * 128));
        out.put((byte) (int) ((float) color.x() * 255f));
        out.put((byte) (int) ((float) color.y() * 255f));
        out.put((byte) (int) ((float) color.z() * 255f));
        out.put((byte) 255);
    }

    private final int vao = GlStateManager._glGenVertexArrays();
    private final int vbo = GlStateManager._glGenBuffers();
    private final int ebo = GlStateManager._glGenBuffers();

    GroundSkyMesh() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        var vertices = MemoryUtil.memAlloc(
                VERTICES * DefaultVertexFormat.POSITION_COLOR.getVertexSize());
        try {
            // Unit-frame directions: the shader rotates them into the observer frame per vertex.
            for (int i = 0; i < VERTICES; i++)
                vertex(vertices, new Point(EAST[i], UP[i], NORTH[i]), new Point(0, 0, 0));
            vertices.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(vertices);
        }
        DefaultVertexFormat.POSITION_COLOR.setupBufferState();
        var indices = MemoryUtil.memAlloc(INDICES.length * Short.BYTES);
        try {
            for (int i : INDICES) indices.putShort((short) i);
            indices.flip();
            GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(indices);
            VertexBuffer.unbind();
        }
    }

    void drawWithShader(Matrix4f matrix, ShaderInstance shader) {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(vao);
        try {
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, matrix,
                    RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            RenderSystem.drawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_SHORT);
            shader.clear();
        } finally {
            VertexBuffer.unbind();
        }
    }

    public void close() {
        GlStateManager._glDeleteBuffers(vbo);
        GlStateManager._glDeleteBuffers(ebo);
        GlStateManager._glDeleteVertexArrays(vao);
    }
}
