package dev.kolustar.probe;

import java.util.function.LongConsumer;

/** Passive draw-call flags and timing, with the development census installed by client wiring. */
public final class ArrivalClientState {
    private static LongConsumer census = start -> {};

    public static void census(LongConsumer listener) {
        census = java.util.Objects.requireNonNull(listener);
    }

    public static void begin() {
        if (ArrivalProbe.ENABLED) {
            dev.kolustar.DevelopmentHooks.remoteDraw = false;
            dev.kolustar.DevelopmentHooks.ownDraw = false;
            dev.kolustar.DevelopmentHooks.parentNanos = 0;
        }
    }

    public static void end(long start) {
        census.accept(start);
    }

    private ArrivalClientState() {}
}
