package dev.kolustar.mixin;

import dev.kolustar.compat.PlanetSky;
import dev.kolustar.sky.WeatherClient;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather2 renders its clouds, funnels and precipitation as ordinary particles, and its render
 * stage only mirrors the resulting rain and thunder amounts onto the client level. Both follow the
 * planet through Kolustar's projected particle shaders, so they keep running in planet levels:
 * {@link Weather2DimensionsMixin} registers the planet for Weather2's weather, cloud, storm and
 * wind systems, and that mirroring is what lets Particle Rain and Weather2 precipitation see the
 * same storm. Only the flat-world fog override is dropped, because the planet atmosphere owns fog.
 * Airless space and above-atmosphere regions stay dry and silent; normal dimensions are unchanged.
 */
@Mixin(targets = "weather2.client.SceneEnhancer", remap = false)
public abstract class Weather2SceneEnhancerMixin {
    /**
     * Weather2's precipitation and cloud particles already ride the projected particle shaders, so
     * they are only suppressed where the planet itself has no weather.
     */
    @Inject(
            method = {"tickEnvironmentalParticleSpawning", "tryParticleSpawning"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void kolustar$skipAbsentPlanetSheets(CallbackInfo ci) {
        if (kolustar$weatherAbsent()) ci.cancel();
    }

    /** Planet atmosphere owns fog; Weather2's overrides would erase it above the shell. */
    @Inject(method = "isFogOverridding", at = @At("HEAD"), cancellable = true, require = 0)
    private static void kolustar$keepAtmosphereFog(CallbackInfoReturnable<Boolean> ci) {
        if (PlanetSky.occludes()) ci.setReturnValue(false);
    }

    private static boolean kolustar$weatherAbsent() {
        return WeatherClient.absent(Minecraft.getInstance().level);
    }

    /**
     * Weather2 drives its own ambient/rain/sandstorm sounds. Mute them where the planet itself
     * has no weather (space, airless worlds, above the atmosphere); breathable rainy surfaces keep
     * Weather2's simulation and sounds.
     */
    @Inject(
            method = {"tickRainSound", "trySoundPlaying"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void kolustar$skipPlanetWeatherAudio(CallbackInfo ci) {
        if (kolustar$weatherAbsent()) ci.cancel();
    }

    @Inject(
            method = {"tryAmbientSounds", "tickSandstormSound", "tickSandstorm"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void kolustar$skipPlanetWeatherAudioStatic(CallbackInfo ci) {
        if (kolustar$weatherAbsent()) ci.cancel();
    }

    @Inject(method = "tryPlayPlayerLockedSound", at = @At("HEAD"), cancellable = true, require = 0)
    private static void kolustar$skipLockedSound(CallbackInfoReturnable<Boolean> ci) {
        if (kolustar$weatherAbsent()) ci.setReturnValue(false);
    }
}
