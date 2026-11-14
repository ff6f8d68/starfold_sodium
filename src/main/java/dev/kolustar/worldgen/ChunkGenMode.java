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

    /** Configured mode from {@code -Dkolustar.chunkGenMode=compatibility|performance}. */
    public static ChunkGenMode configured() {
        if (MAC_OS || DEDICATED_SERVER) return COMPATIBILITY;
        String raw = System.getProperty("kolustar.chunkGenMode", "performance").trim();
        for (ChunkGenMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw)) {
                // On non-macOS client devices that support GL 4.3, performance mode is forced on.
                if (mode == COMPATIBILITY && computeCapableRuntime()) return PERFORMANCE;
                return mode;
            }
        }
        LOGGER.warn("Unknown kolustar.chunkGenMode '{}', using performance when available", raw);
        return PERFORMANCE;
    }

    /** Whether this runtime is a non-macOS client platform where GL 4.3 compute can be used. */
    public static boolean computeCapableRuntime() {
        return !MAC_OS && !DEDICATED_SERVER;
    }

    /** Effective mode: PERFORMANCE forced off on macOS/dedicated servers, forced on for non-macOS GL 4.3-capable clients. */
    public static ChunkGenMode effective() {
        if (MAC_OS || DEDICATED_SERVER) return COMPATIBILITY;
        return supportsComputeShaders() ? PERFORMANCE : COMPATIBILITY;
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
        if (MAC_OS || DEDICATED_SERVER) return COMPATIBILITY;
        // On non-macOS client devices that support GL 4.3, performance mode is forced on.
        return PERFORMANCE;
    }
}
