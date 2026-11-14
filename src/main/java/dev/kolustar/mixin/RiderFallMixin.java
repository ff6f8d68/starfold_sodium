package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.kolustar.ship.RiderFall;
import dev.kolustar.ship.RiderFallContext;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Keep vanilla landing/damage handling, but do not count the deck's motion as a rider's fall. */
@Mixin(ServerPlayer.class)
public abstract class RiderFallMixin {
    @Unique private final RiderFall kolustar$riderFall = new RiderFall();

    @WrapMethod(method = "doCheckFallDamage")
    private void kolustar$relativeFall(
            double x, double y, double z, boolean ground, Operation<Void> original) {
        original.call(
                x,
                RiderFallContext.displacement((ServerPlayer) (Object) this, kolustar$riderFall, y),
                z,
                ground);
    }
}
