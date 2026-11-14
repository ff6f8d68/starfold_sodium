package dev.kolustar.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.kolustar.ship.Thrusters;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Include fuel in native mass uploads, including plot load before the saved pose is restored. */
@Mixin(value = ServerSubLevel.class, remap = false)
public abstract class SableFuelMassMixin {
    @Inject(method = "updateMergedMassData(F)V", at = @At("HEAD"), remap = false)
    private void kolustar$fuelMass(float partialPhysicsTick, CallbackInfo ci) {
        Thrusters.updateFuelMass((ServerSubLevel) (Object) this);
    }
}
