package dev.kolustar.render;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Optional Distant Horizons integration / kill switch for Kolustar's custom far-terrain LODs.
 * When DH is present (or LODs are explicitly disabled), Kolustar defers far terrain and skips
 * its own CPU LOD/cover mesh generation and drawing.
 */
public final class DistantHorizonsSupport {
    private static final boolean FORCE_DISABLED =
            "false".equalsIgnoreCase(System.getProperty("kolustar.lod", "true"))
                    || "false".equalsIgnoreCase(System.getProperty("kolustar.farTerrain", "true"));
    private static Boolean loaded;

    private DistantHorizonsSupport() {}

    public static boolean loaded() {
        Boolean value = loaded;
        if (value == null) {
            boolean present = false;
            try {
                if (FMLEnvironment.dist.isClient()) {
                    var mods = ModList.get();
                    present =
                            mods.isLoaded("distanthorizons")
                                    || mods.isLoaded("distant_horizons")
                                    || mods.isLoaded("lod");
                }
            } catch (Throwable ignored) {
            }
            loaded = value = present;
        }
        return value;
    }

    /** Kolustar's custom CPU LOD/cover meshing and rendering is disabled. */
    public static boolean useExternalLods() {
        return FORCE_DISABLED || loaded();
    }
}
