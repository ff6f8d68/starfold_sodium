package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;

import dev.kolustar.sky.Weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.1 places weather lightning inside tickChunk, not a separate tickThunder method. */
@Mixin(ServerLevel.class)
public abstract class WeatherThunderMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void kolustar$boltProbe(
            net.minecraft.world.entity.Entity entity,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (Boolean.getBoolean("kolustar.carryProbe")
                && entity instanceof net.minecraft.world.entity.LightningBolt)
            org.slf4j.LoggerFactory.getLogger("kolustar-probe")
                    .info(
                            "PROBE WEATHER BOLT region {} pos {} wall {}",
                            dev.kolustar.sky.Space.in(entity),
                            entity.position(),
                            System.currentTimeMillis());
    }

    @WrapOperation(
            method = "tickChunk",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean kolustar$thunder(ServerLevel level, BlockPos pos, Operation<Boolean> original) {
        return !Weather.absent(level, pos) && original.call(level, pos);
    }
}
