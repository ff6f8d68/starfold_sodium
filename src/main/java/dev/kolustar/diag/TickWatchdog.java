package dev.kolustar.diag;

import com.mojang.logging.LogUtils;
import dev.kolustar.universe.SystemChunkGenerator;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Reports server ticks that miss the 20 TPS budget together with the chunk generation seen in the
 * same window, so a stall can be attributed without a profiler. Costs two clock reads per tick.
 */
public final class TickWatchdog {
    private static long started;
    private static long worst;
    private static long windowStart;
    private static long lastGenerated;
    private static long lastNanos;

    public static void initialize() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> started = System.nanoTime());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> finished());
    }

    private static void finished() {
        if (started == 0) return;
        long now = System.nanoTime();
        long ms = (now - started) / 1_000_000;
        if (ms > worst) worst = ms;
        if (windowStart == 0) windowStart = now;
        if (now - windowStart < 5_000_000_000L) return;
        long generated = SystemChunkGenerator.GENERATED.sum();
        long nanos = SystemChunkGenerator.NANOS.sum();
        long chunks = generated - lastGenerated;
        long chunkMs = (nanos - lastNanos) / 1_000_000;
        if (worst >= 200)
            LogUtils.getLogger()
                    .info(
                            "[Kolustar] worst tick {} ms since last report; {} chunks generated in {} ms",
                            worst,
                            chunks,
                            chunkMs);
        worst = 0;
        windowStart = now;
        lastGenerated = generated;
        lastNanos = nanos;
    }

    private TickWatchdog() {}
}
