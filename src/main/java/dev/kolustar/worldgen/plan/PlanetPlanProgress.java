package dev.kolustar.worldgen.plan;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Progress for the currently generating planet plan, for loading UI. */
public final class PlanetPlanProgress {
    public record State(int planet, String stage, int done, int total) {
        public int percent() { return total <= 0 ? -1 : Math.min(100, Math.max(0, done * 100 / total)); }
    }
    private static final AtomicReference<State> STATE = new AtomicReference<>();
    static void start(int planet, String stage, int total) { STATE.set(new State(planet, stage, 0, Math.max(0, total))); }
    static void update(String stage, int done, int total) {
        State s = STATE.get();
        if (s != null) STATE.set(new State(s.planet(), stage, done, Math.max(0, total)));
    }
    static void clear() { STATE.set(null); }
    public static State get() { return STATE.get(); }
    private PlanetPlanProgress() {}
}
