package dev.kolustar.render;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Far-terrain selection for Kolustar's curved planet charts.
 *
 * <p>Distant Horizons builds its LODs from vanilla world assumptions and does not follow the
 * planet chart projection, so its distant terrain does not line up with Kolustar's surface and
 * can appear as detached sheets or holes over real chunks. Kolustar's own far terrain therefore
 * stays enabled when DH is installed; {@code -Dkolustar.deferToDistantHorizons=true} opts back in
 * to skipping it. {@code -Dkolustar.lod=false} still disables Kolustar far terrain entirely.
 */
public final class DistantHorizonsSupport {
    private static final boolean FORCE_DISABLED =
            "false".equalsIgnoreCase(System.getProperty("kolustar.lod", "true"))
                    || "false".equalsIgnoreCase(System.getProperty("kolustar.farTerrain", "true"));
    private static final boolean DEFER =
            "true".equalsIgnoreCase(System.getProperty("kolustar.deferToDistantHorizons", "false"));
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
            if (present)
                com.mojang.logging.LogUtils.getLogger()
                        .info(
                                "Distant Horizons detected; Kolustar far terrain {}",
                                DEFER
                                        ? "deferred to DH (-Dkolustar.deferToDistantHorizons=true)"
                                        : "stays enabled because DH LODs do not follow the planet chart"
                                                + " (use -Dkolustar.deferToDistantHorizons=true to defer)");
        }
        return value;
    }

    /** Kolustar's custom CPU LOD/cover meshing and rendering is disabled. */
    public static boolean useExternalLods() {
        return FORCE_DISABLED || (DEFER && loaded());
    }
}
