package dev.kolustar.worldgen;

import java.util.Locale;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Selects how planet chunk blocks are generated. */
public enum ChunkGenMode {
    COMPATIBILITY,
    PERFORMANCE;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean MAC_OS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
                    || System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("darwin");

    public static boolean isMacOs() {
        return MAC_OS;
    }

    /** Configured mode from {@code -Dkolustar.chunkGenMode=compatibility|performance}. */
    public static ChunkGenMode configured() {
        String raw = System.getProperty("kolustar.chunkGenMode", "compatibility").trim();
        for (ChunkGenMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw)) return mode;
        }
        LOGGER.warn("Unknown kolustar.chunkGenMode '{}', using compatibility", raw);
        return COMPATIBILITY;
    }

    /** Effective mode for this runtime: PERFORMANCE is forced off on macOS and without GL 4.3 compute. */
    public static ChunkGenMode effective() {
        ChunkGenMode mode = configured();
        if (mode == PERFORMANCE && MAC_OS) {
            return COMPATIBILITY;
        }
        return mode;
    }

    /** Whether the current GL context can run chunk-generation compute shaders. */
    public static boolean supportsComputeShaders() {
        if (MAC_OS) return false;
        try {
            GLCapabilities caps = GL.getCapabilities();
            return caps != null && caps.OpenGL43;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** PERFORMANCE is only usable when compute is available on a non-macOS runtime. */
    public static boolean performanceAvailable() {
        return !MAC_OS && supportsComputeShaders();
    }

    public static ChunkGenMode forChunkGeneration() {
        ChunkGenMode mode = configured();
        if (mode == PERFORMANCE) {
            if (MAC_OS) return COMPATIBILITY;
            // Chunk generation runs on server/background threads without a guaranteed GL context.
            // Keep deterministic CPU generation unless a future GL-context compute path is wired.
            if (!supportsComputeShaders()) return COMPATIBILITY;
        }
        return mode;
    }
}
