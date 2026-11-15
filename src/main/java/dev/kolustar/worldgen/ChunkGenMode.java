package dev.kolustar.worldgen;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

/** Selects how planet chunk blocks are generated. */
public enum ChunkGenMode {
    COMPATIBILITY,
    PERFORMANCE;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean MAC_OS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
                    || System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("darwin");
    private static final boolean DEDICATED_SERVER = FMLEnvironment.dist == Dist.DEDICATED_SERVER;

    public static boolean isDedicatedServer() {
        return DEDICATED_SERVER;
    }

    public static boolean isMacOs() {
        return MAC_OS;
    }

    /** Configured mode from the common config's {@code generation.gpuChunkGeneration}. */
    public static ChunkGenMode configured() {
        if (MAC_OS || DEDICATED_SERVER) return COMPATIBILITY;
        return dev.kolustar.KolustarConfig.gpuChunkGeneration() ? PERFORMANCE : COMPATIBILITY;
    }

    /** Whether this runtime is a non-macOS client platform where GL 4.3 compute can be used. */
    public static boolean computeCapableRuntime() {
        return !MAC_OS && !DEDICATED_SERVER;
    }

    /** Effective mode: PERFORMANCE only when enabled in the config on a GL 4.3-capable client. */
    public static ChunkGenMode effective() {
        if (MAC_OS || DEDICATED_SERVER) return COMPATIBILITY;
        return supportsComputeShaders() && dev.kolustar.KolustarConfig.gpuChunkGeneration()
                ? PERFORMANCE
                : COMPATIBILITY;
    }

    /** Whether the current GL context can run chunk-generation compute shaders. */
    public static boolean supportsComputeShaders() {
        if (MAC_OS || DEDICATED_SERVER) return false;
        try {
            GLCapabilities caps = GL.getCapabilities();
            return caps != null && caps.OpenGL43;
        } catch (Throwable ignored) {
            // No current GL context or GL unavailable.
            return false;
        }
    }

    /** PERFORMANCE is intended for non-macOS GL 4.3-capable client runtimes. */
    public static boolean performanceAvailable() {
        return !MAC_OS && !DEDICATED_SERVER;
    }

    public static ChunkGenMode forChunkGeneration() {
        return configured();
    }
}
