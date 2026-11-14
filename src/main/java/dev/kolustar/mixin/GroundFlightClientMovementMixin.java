package dev.kolustar.mixin;

import dev.kolustar.flight.GroundFlightClient;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class GroundFlightClientMovementMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void kolustar$nativeMove(CallbackInfo ci) {
        if (GroundFlightClient.controlled()) ci.cancel();
    }
}
