package dev.kolustar.mixin;

import dev.kolustar.flight.GroundFlightClient;
import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.render.GroundClient;
import dev.kolustar.space.SpaceClient;

import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native block/hand skylight uses the same physical Sun; vanilla block light stays intact. */
@Mixin(ClientLevel.class)
public abstract class GroundLightMixin {
    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void kolustar$nativeSun(float partial, CallbackInfoReturnable<Float> ci) {
        if (SpaceClient.active()) {
            ci.setReturnValue((float) (.2 + .8 * SpaceClient.sunlight()));
            return;
        }
        if (PlanetLevels.is((ClientLevel) (Object) this) && GroundClient.ready())
            ci.setReturnValue(
                    (float)
                            (.2
                                    + .8
                                            * (GroundFlightClient.controlled()
                                                    ? GroundClient.sunlight()
                                                    : GroundClient.daylight())));
    }
}
