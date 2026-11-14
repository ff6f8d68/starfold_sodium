package dev.kolustar.mixin;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundTeleportEntityPacket.class)
public interface TeleportPacketAccess {
    @Mutable
    @Accessor("x")
    void kolustar$coverX(double x);

    @Mutable
    @Accessor("z")
    void kolustar$coverZ(double z);

    @Mutable
    @Accessor("yRot")
    void kolustar$coverYaw(byte yaw);
}
