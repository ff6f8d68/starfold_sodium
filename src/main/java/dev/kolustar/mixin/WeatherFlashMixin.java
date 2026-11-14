package dev.kolustar.mixin;

import dev.kolustar.sky.WeatherClient;

import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class WeatherFlashMixin {
    @Inject(method = "getSkyFlashTime", at = @At("HEAD"), cancellable = true)
    private void kolustar$flash(CallbackInfoReturnable<Integer> ci) {
        if (WeatherClient.absent((ClientLevel) (Object) this)) ci.setReturnValue(0);
    }
}
