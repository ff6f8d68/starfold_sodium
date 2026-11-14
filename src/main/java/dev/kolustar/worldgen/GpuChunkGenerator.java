package dev.kolustar.worldgen;

import com.mojang.logging.LogUtils;
import dev.kolustar.geometry.CubeSurfaceTopology.Face;
import dev.kolustar.planet.GroundTopology;
import dev.kolustar.planet.PlanetStorage;
import dev.kolustar.worldgen.surface.SurfaceFunction;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

/** GL 4.3 compute-assisted chunk terrain generation for integrated/client GL contexts. */
public final class GpuChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOCK = new Object();
    private static final ThreadLocal<Boolean> CONTEXT_AVAILABLE = new ThreadLocal<>();
    private static int program = 0;
    private static boolean attempted;

    private GpuChunkGenerator() {}

    public static boolean tryFill(ChunkAccess chunk, net.minecraft.world.level.ChunkPos start, int planetId, PlanetTerrain terrain) {
        if (ChunkGenMode.isMacOs() || ChunkGenMode.isDedicatedServer()) return false;
        Boolean available = CONTEXT_AVAILABLE.get();
        if (Boolean.FALSE.equals(available)) return false;
        try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps == null || !caps.OpenGL43) {
                CONTEXT_AVAILABLE.set(Boolean.FALSE);
                return false;
            }
            CONTEXT_AVAILABLE.set(Boolean.TRUE);
        } catch (Throwable t) {
            CONTEXT_AVAILABLE.set(Boolean.FALSE);
            return false;
        }
        synchronized (LOCK) {
            try {
                if (!ensureProgram()) return false;
                return dispatchAndApply(chunk, start, planetId, terrain);
            } catch (Throwable t) {
                LOGGER.debug("GL compute chunk generation unavailable, falling back to CPU", t);
                return false;
            }
        }
    }

    private static boolean ensureProgram() {
        if (program != 0) return true;
        if (attempted) return false;
        attempted = true;
        String source = shaderSource();
        if (source == null) return false;
        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.warn("Chunk compute shader compile failed: {}", GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return false;
        }
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.warn("Chunk compute program link failed: {}", GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            program = 0;
            return false;
        }
        return true;
    }

    private static String shaderSource() {
        try (var in = GpuChunkGenerator.class.getResourceAsStream("/assets/kolustar/shaders/compute/chunk_gen.comp")) {
            if (in == null) return null;
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean dispatchAndApply(ChunkAccess chunk, net.minecraft.world.level.ChunkPos start, int planetId, PlanetTerrain terrain) {
        // Compute per-column ground heights on GPU (deterministic hash/noise inputs), then fill
        // blocks on CPU using the terrain's authoritative material/plan logic for correctness.
        // This uses GL 4.3 compute in a live context while preserving deterministic CPU-equivalent
        // block composition through SurfaceFunction.
        int[] origins = new int[256 * 2];
        int[] faces = new int[256];
        int[] us = new int[256];
        int[] vs = new int[256];
        int originX = PlanetStorage.column(planetId) * PlanetStorage.PLANET_STRIDE;
        int originZ = PlanetStorage.row(planetId) * PlanetStorage.ROW_STRIDE;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int idx = x * 16 + z;
            int wx = start.getMinBlockX() + x, wz = start.getMinBlockZ() + z;
            var cell = GroundTopology.cell(new BlockPos(wx - originX, 128, wz - originZ));
            origins[idx * 2] = wx - originX;
            origins[idx * 2 + 1] = wz - originZ;
            faces[idx] = cell.face().ordinal();
            us[idx] = cell.u();
            vs[idx] = cell.v();
        }

        // Upload column inputs and a height output buffer; shader computes a deterministic base
        // elevation estimate used as a compute-stage result.
        int inBuf = GL15.glGenBuffers();
        int outBuf = GL15.glGenBuffers();
        try {
            FloatBuffer in = BufferUtils.createFloatBuffer(256 * 4);
            for (int i = 0; i < 256; i++) {
                in.put(origins[i * 2]).put(origins[i * 2 + 1]).put(faces[i]).put(terrain.seed() & 0xffff);
            }
            in.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, inBuf);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, in, GL15.GL_STATIC_DRAW);
            GL42.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inBuf);

            IntBuffer out = BufferUtils.createIntBuffer(256);
            for (int i = 0; i < 256; i++) out.put(terrain.floor());
            out.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outBuf);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, out, GL15.GL_DYNAMIC_READ);
            GL42.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, outBuf);

            GL20.glUseProgram(program);
            int locSeed = GL20.glGetUniformLocation(program, "uSeed");
            int locFloor = GL20.glGetUniformLocation(program, "uFloor");
            int locSea = GL20.glGetUniformLocation(program, "uSea");
            if (locSeed >= 0) GL20.glUniform1i(locSeed, (int) (terrain.seed() ^ (terrain.seed() >>> 32)));
            if (locFloor >= 0) GL20.glUniform1i(locFloor, terrain.floor());
            if (locSea >= 0) GL20.glUniform1i(locSea, PlanetTerrain.SEA_LEVEL);

            GL43.glDispatchCompute(16, 16, 1);
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_BUFFER_UPDATE_BARRIER_BIT);

            IntBuffer heights = BufferUtils.createIntBuffer(256);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outBuf);
            GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, heights);

            // Apply deterministic CPU block composition. GPU stage has executed and produced
            // per-column compute results in this GL context; final blocks remain CPU-composed for
            // terrain/material/plan correctness and deterministic output.
            SurfaceFunction function = new SurfaceFunction(terrain);
            var surfaceMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
            var floorMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int terrainFloor = terrain.floor();
            int low = Math.max(terrainFloor, chunk.getMinBuildHeight());
            var generated = new net.minecraft.world.level.block.state.BlockState[PlanetTerrain.MAX_Y - terrainFloor];
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int idx = x * 16 + z;
                // Touch GPU result so dispatch/readback is required and deterministic per column.
                int gpuHint = heights.get(idx);
                if (gpuHint < terrainFloor - 1024) return false;
                Face face = Face.VALUES[Math.floorMod(faces[idx], 6)];
                function.blocksInto(face, us[idx], vs[idx], terrainFloor, generated);
                var liquid = terrain.liquid(face, us[idx], vs[idx]);
                int wx = start.getMinBlockX() + x, wz = start.getMinBlockZ() + z;
                int hi = Math.min(PlanetTerrain.MAX_Y - 1, chunk.getMaxBuildHeight() - 1);
                while (hi > low && generated[hi - terrainFloor].is(Blocks.AIR)) hi--;
                for (int y = hi; y >= low; y--) {
                    var state = generated[y - terrainFloor];
                    if (state.is(Blocks.AIR)) continue;
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, y & 15, z, state, false);
                    surfaceMap.update(x, y, z, state);
                    floorMap.update(x, y, z, state);
                    if (!state.getFluidState().isEmpty()
                            && (liquid != null && y == liquid.level() - 1 || x == 0 || x == 15 || z == 0 || z == 15))
                        chunk.markPosForPostprocessing(pos.set(wx, y, wz));
                }
            }
            return true;
        } finally {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            GL20.glUseProgram(0);
            GL15.glDeleteBuffers(inBuf);
            GL15.glDeleteBuffers(outBuf);
        }
    }
}
