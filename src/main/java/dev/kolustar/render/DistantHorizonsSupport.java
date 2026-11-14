package dev.kolustar.render;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/** Optional Distant Horizons integration. When present, Kolustar defers custom far LOD work to DH. */
public final class DistantHorizonsSupport {
    private static Boolean loaded;

    private DistantHorizonsSupport() {}

    public static boolean loaded() {
        Boolean value = loaded;
        if (value == null) {
            boolean present = false;
            try {
                present = FMLEnvironment.dist.isClient() && ModList.get().isLoaded("distanthorizons");
            } catch (Throwable ignored) {
            }
            loaded = value = present;
        }
        return value;
    }

    /** Kolustar's custom CPU LOD meshing/rendering is disabled when DH is active. */
    public static boolean useExternalLods() {
        return loaded();
    }
}
