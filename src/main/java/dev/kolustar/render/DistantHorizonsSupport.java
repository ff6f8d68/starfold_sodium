package dev.kolustar.render;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Far-terrain selection for Kolustar's curved planet charts.
 *
 * <p>Distant Horizons builds its LODs from vanilla world assumptions and does not follow the
 * planet chart projection, so its distant terrain does not line up with Kolustar's surface and
 * can appear as detached sheets or holes over real chunks. Kolustar's own far terrain therefore
 * stays enabled when DH is installed; the client config's
 * {@code farTerrain.deferToDistantHorizons} opts back in to skipping it, and
 * {@code farTerrain.enabled=false} disables Kolustar far terrain entirely.
 */
public final class DistantHorizonsSupport {
    private static boolean forceDisabled() {
        return !dev.kolustar.KolustarConfig.farTerrain();
    }

    private static boolean defer() {
        return dev.kolustar.KolustarConfig.deferToDistantHorizons();
    }
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
                                defer()
                                        ? "deferred to Distant Horizons (config: farTerrain.deferToDistantHorizons)"
                                        : "stays enabled because DH LODs do not follow the planet chart");
        }
        return value;
    }

    /** Kolustar's custom CPU LOD/cover meshing and rendering is disabled. */
    public static boolean useExternalLods() {
        return forceDisabled() || (defer() && loaded());
    }
}
