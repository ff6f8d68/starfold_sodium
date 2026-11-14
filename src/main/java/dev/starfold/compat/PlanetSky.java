package dev.starfold.compat;

import dev.starfold.planet.PlanetLevels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Shared gate for third-party weather passes that place vertices in flat world space. Inside a
 * Starfold planet such a pass would be painted through the curved shell instead of above it.
 * Only those foreign sky passes are skipped; particles, wind, sounds, radar and gameplay keep
 * running, and every non-planet level behaves as before.
 */
public final class PlanetSky {
    /** The local client stands in a Starfold planet level, where flat-world skies do not apply. */
    public static boolean occludes() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && PlanetLevels.is(level);
    }

    private PlanetSky() {}
}
