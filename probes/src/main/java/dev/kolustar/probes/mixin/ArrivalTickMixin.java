package dev.kolustar.probes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.kolustar.probe.ArrivalProbe;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class ArrivalTickMixin {
    @WrapMethod(method = "tickServer")
    private void kolustar$arrivalTick(BooleanSupplier time, Operation<Void> original) {
        if (!ArrivalProbe.ENABLED) {
            long start = dev.kolustar.probe.RelightProbe.ENABLED ? System.nanoTime() : 0;
            try {
                original.call(time);
            } finally {
                dev.kolustar.probe.RelightProbe.tick(
                        ((MinecraftServer) (Object) this).getTickCount(), start);
            }
            return;
        }
        ArrivalProbe.tickStart();
        long start = ArrivalProbe.start();
        try {
            original.call(time);
        } finally {
            ArrivalProbe.tickEnd(((MinecraftServer) (Object) this).getTickCount(), start);
        }
    }
}
