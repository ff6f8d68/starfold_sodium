package dev.kolustar.mixin;

import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientboundSableSnapshotDualPacket.class, remap = false)
public interface SableSnapshotAccess {
    @Accessor(value = "interpolationTick", remap = false)
    int kolustar$interpolationTick();
}
