package dev.starfold.mixin;

import dev.starfold.sky.WeatherClient;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle Rain keeps its own rain state and spawns around the camera. A Starfold planet can be
 * airless, above its atmosphere or in space while the level's vanilla rain flag is still set, so
 * it would snow/rain there. Ask Starfold's planet weather instead of the flat level state; the
 * mod's particles otherwise render through the projected particle core shader unchanged.
 */
@Mixin(targets = "pigcart.particlerain.ParticleSpawner", remap = false)
public abstract class ParticleRainSpawnerMixin {
    @Inject(
            method = "tick(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void starfold$respectPlanetWeather(
            ClientLevel level, Vec3 camera, CallbackInfo ci) {
        if (WeatherClient.absent(level)) ci.cancel();
    }
}
