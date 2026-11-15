package dev.kolustar;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Kolustar tuning. Values live in {@code config/kolustar-client.toml} and
 * {@code config/kolustar-common.toml} and can be edited in game from the mod list's
 * Config button; no JVM arguments are needed.
 *
 * <p>Values are read through the accessors so a missing/unloaded spec falls back to the
 * documented default instead of throwing during early class initialisation.
 */
public final class KolustarConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;

    private static final ModConfigSpec.BooleanValue BLOOM;
    private static final ModConfigSpec.DoubleValue BLOOM_THRESHOLD;
    private static final ModConfigSpec.DoubleValue BLOOM_KNEE;
    private static final ModConfigSpec.DoubleValue BLOOM_STRENGTH;
    private static final ModConfigSpec.BooleanValue FAR_TERRAIN;
    private static final ModConfigSpec.BooleanValue DEFER_DISTANT_HORIZONS;
    private static final ModConfigSpec.IntValue LOD_THREADS;
    private static final ModConfigSpec.IntValue SURFACE_THREADS;
    private static final ModConfigSpec.IntValue COVER_STEP;

    private static final ModConfigSpec.BooleanValue PLANET_STRUCTURES;
    private static final ModConfigSpec.IntValue PLAN_BUDGET_MS;
    private static final ModConfigSpec.BooleanValue GPU_CHUNK_GENERATION;
    private static final ModConfigSpec.BooleanValue HBM_COMPAT;

    static {
        var client = new ModConfigSpec.Builder();
        client.comment("Client rendering. Most changes apply within a frame; thread counts apply on restart.")
                .push("rendering");
        BLOOM =
                client.comment(
                                "Bloom post-process.",
                                "Off by default: the post pass is not validated against every Sodium/Iris/Veil pipeline.")
                        .define("bloom", false);
        BLOOM_THRESHOLD =
                client.comment("Brightness where bloom starts.")
                        .defineInRange("bloomThreshold", 0.72D, 0.0D, 8.0D);
        BLOOM_KNEE =
                client.comment("Softness of the bloom threshold.")
                        .defineInRange("bloomKnee", 0.35D, 0.0001D, 8.0D);
        BLOOM_STRENGTH =
                client.comment("Bloom composite strength.")
                        .defineInRange("bloomStrength", 0.9D, 0.0D, 8.0D);
        client.pop();

        client.comment("Distant terrain").push("farTerrain");
        FAR_TERRAIN =
                client.comment(
                                "Draw Kolustar's own far terrain and level-of-detail meshes.",
                                "Off by default: those LODs cost client CPU and memory, and Distant Horizons",
                                "already draws far terrain when it is installed. Turn on to get distant ground",
                                "without DH.")
                        .define("enabled", false);
        DEFER_DISTANT_HORIZONS =
                client.comment(
                                "Let Distant Horizons draw far terrain instead of Kolustar.",
                                "DH builds LODs on flat-world assumptions and does not follow the curved planet",
                                "chart, so its terrain can appear as detached sheets. Off by default.")
                        .define("deferToDistantHorizons", false);
        client.pop();

        client.comment("Background meshing").push("work");
        LOD_THREADS =
                client.comment("LOD meshing threads. 0 = automatic (a quarter of the cores, at most two).")
                        .defineInRange("lodThreads", 0, 0, 8);
        SURFACE_THREADS =
                client.comment("Planet surface build threads. 0 = automatic (at most two).")
                        .defineInRange("surfaceThreads", 0, 0, 8);
        COVER_STEP =
                client.comment("Smallest far-terrain cell in blocks. Larger is cheaper and coarser.")
                        .defineInRange("coarseStep", 16, 16, 128);
        client.pop();
        CLIENT_SPEC = client.build();

        var common = new ModConfigSpec.Builder();
        common.comment("World generation and runtime tuning.").push("generation");
        PLANET_STRUCTURES =
                common.comment(
                                "Planet-wide structure planning (villages, ruins, their recipes).",
                                "Turning this off removes the plan pass entirely and the joining stall with it.")
                        .define("planetStructures", true);
        PLAN_BUDGET_MS =
                common.comment(
                                "Total milliseconds a planet plan may spend surveying and placing structures.",
                                "Lower values start worlds faster and place fewer structures; plans are cached",
                                "after the first generation, so this only affects new or regenerated planets.")
                        .defineInRange("planBudgetMs", 50, 10, 600000);
        GPU_CHUNK_GENERATION =
                common.comment(
                                "Use the GL 4.3 compute path for chunk generation when a client GL context is",
                                "available.",
                                "World generation runs on worker threads where Minecraft's GL context is not",
                                "current, so the compute stage only engages where a context exists; otherwise",
                                "the parallel CPU fill generates the chunk. The compute stage currently",
                                "produces column hints that the CPU composition does not consume, so it does",
                                "not reduce CPU work yet.")
                        .define("gpuChunkGeneration", true);
        common.pop();
        common.comment("Mod compatibility").push("compatibility");
        HBM_COMPAT =
                common.comment(
                                "Apply Kolustar's HBM Continuum compatibility mixins.",
                                "Read once at startup, before HBM classes load.")
                        .define("hbmCompat", true);
        common.pop();
        COMMON_SPEC = common.build();
    }

    public static boolean bloom() {
        return bool(CLIENT_SPEC, BLOOM, false);
    }

    public static double bloomThreshold() {
        return decimal(CLIENT_SPEC, BLOOM_THRESHOLD, 0.72D);
    }

    public static double bloomKnee() {
        return decimal(CLIENT_SPEC, BLOOM_KNEE, 0.35D);
    }

    public static double bloomStrength() {
        return decimal(CLIENT_SPEC, BLOOM_STRENGTH, 0.9D);
    }

    public static boolean farTerrain() {
        return bool(CLIENT_SPEC, FAR_TERRAIN, false);
    }

    public static boolean deferToDistantHorizons() {
        return bool(CLIENT_SPEC, DEFER_DISTANT_HORIZONS, false);
    }

    public static int lodThreads() {
        return integer(CLIENT_SPEC, LOD_THREADS, 0);
    }

    public static int surfaceThreads() {
        return integer(CLIENT_SPEC, SURFACE_THREADS, 0);
    }

    public static int coarseStep() {
        return integer(CLIENT_SPEC, COVER_STEP, 16);
    }

    public static boolean planetStructures() {
        return bool(COMMON_SPEC, PLANET_STRUCTURES, true);
    }

    public static long planBudgetMs() {
        return integer(COMMON_SPEC, PLAN_BUDGET_MS, 50);
    }

    public static boolean gpuChunkGeneration() {
        return bool(COMMON_SPEC, GPU_CHUNK_GENERATION, true);
    }

    public static boolean hbmCompat() {
        return bool(COMMON_SPEC, HBM_COMPAT, true);
    }

    private static boolean bool(
            ModConfigSpec spec, ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return spec.isLoaded() ? value.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int integer(ModConfigSpec spec, ModConfigSpec.IntValue value, int fallback) {
        try {
            return spec.isLoaded() ? value.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static double decimal(
            ModConfigSpec spec, ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return spec.isLoaded() ? value.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private KolustarConfig() {}
}
