package dev.kolustar.mixin;

import dev.kolustar.planet.PlanetLevels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Weather2 simulates weather, clouds, storms and wind effects only in the dimensions listed in its
 * config, and that default list is the overworld alone. The Kolustar planet level is an ordinary
 * overworld-like level, so without an entry Weather2 never creates a weather manager there: no
 * storm, no cloud particles and no precipitation would ever reach a planet regardless of what the
 * client rendering hooks allow. The planet dimension is appended after every config parse, so
 * planets behave like the overworld while every user-edited list entry keeps working unchanged.
 */
@Mixin(targets = "weather2.config.WeatherUtilConfig", remap = false)
public abstract class Weather2DimensionsMixin {
    @Shadow public static List<String> listDimensionsWeather;

    @Shadow public static List<String> listDimensionsClouds;

    @Shadow public static List<String> listDimensionsStorms;

    @Shadow public static List<String> listDimensionsWindEffects;

    @Inject(method = "processLists", at = @At("RETURN"), require = 0)
    private static void kolustar$includePlanet(CallbackInfo ci) {
        // One level holds every planet body; Weather2's per-level manager serves all of them.
        String planet = PlanetLevels.PLANET.location().toString();
        kolustar$include(listDimensionsWeather, planet);
        kolustar$include(listDimensionsClouds, planet);
        kolustar$include(listDimensionsStorms, planet);
        kolustar$include(listDimensionsWindEffects, planet);
    }

    private static void kolustar$include(List<String> dimensions, String planet) {
        if (dimensions != null && !dimensions.contains(planet)) dimensions.add(planet);
    }
}
