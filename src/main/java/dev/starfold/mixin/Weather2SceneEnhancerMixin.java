package dev.starfold.mixin;

import dev.starfold.compat.PlanetSky;
import dev.starfold.sky.WeatherClient;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather2 draws clouds, storm walls, tornado funnels and precipitation sheets with its own
 * world-space renderers. That space is bent into a sphere on a Starfold planet, so those passes
 * would be painted through the ground and through the atmosphere shell. Suppress only the
 * rendering and sheet spawning inside planet levels: Weather2's simulation, wind, sounds, radar
 * and all of its particles (drawn by the projected particle core shader) continue to run, and
 * normal dimensions stay untouched.
 */
@Mixin(targets = "weather2.client.SceneEnhancer", remap = false)
public abstract class Weather2SceneEnhancerMixin {
    @Inject(
            method = "renderTick(Lnet/neoforged/neoforge/client/event/RenderLevelStageEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void starfold$skipPlanetSky(RenderLevelStageEvent event, CallbackInfo ci) {
        if (PlanetSky.occludes()) ci.cancel();
    }

    @Inject(
            method = {"tickEnvironmentalParticleSpawning", "tryParticleSpawning"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void starfold$skipPlanetSheets(CallbackInfo ci) {
        if (PlanetSky.occludes()) ci.cancel();
    }

    /** Planet atmosphere owns fog; Weather2's overrides would erase it above the shell. */
    @Inject(method = "isFogOverridding", at = @At("HEAD"), cancellable = true, require = 0)
    private static void starfold$keepAtmosphereFog(CallbackInfoReturnable<Boolean> ci) {
        if (PlanetSky.occludes()) ci.setReturnValue(false);
    }

    private static boolean starfold$weatherAbsent() {
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
    private void starfold$skipPlanetWeatherAudio(CallbackInfo ci) {
        if (starfold$weatherAbsent()) ci.cancel();
    }

    @Inject(
            method = {"tryAmbientSounds", "tickSandstormSound", "tickSandstorm"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void starfold$skipPlanetWeatherAudioStatic(CallbackInfo ci) {
        if (starfold$weatherAbsent()) ci.cancel();
    }

    @Inject(method = "tryPlayPlayerLockedSound", at = @At("HEAD"), cancellable = true, require = 0)
    private static void starfold$skipLockedSound(CallbackInfoReturnable<Boolean> ci) {
        if (starfold$weatherAbsent()) ci.setReturnValue(false);
    }
}
