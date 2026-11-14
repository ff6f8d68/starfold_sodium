package dev.kolustar.mixin;

import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import dev.kolustar.ground.PayloadMap;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Sable's datagrams do not pass the player's connection, where every other packet is relabelled for its observer. */
@Mixin(value = SableUDPServer.class, remap = false)
public abstract class SableUdpMixin {
    @Inject(method = "sendUDPPacket", at = @At("HEAD"), cancellable = true, remap = false)
    private void kolustar$orderedCarry(
            ServerPlayer player,
            SableUDPPacket packet,
            boolean flush,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (dev.kolustar.ship.SableFrames.reliable(player)
                && packet
                        instanceof
                        dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket
                                snapshot) {
            // The argument hook leaves this route alone; map once, then send behind the carry on
            // TCP.
            dev.kolustar.ground.PacketMap.raw(
                    player,
                    new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                            PayloadMap.relabel(player, snapshot)));
            ci.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "sendUDPPacket", at = @At("HEAD"), argsOnly = true, remap = false)
    private SableUDPPacket kolustar$nativeFrame(SableUDPPacket packet, ServerPlayer player) {
        return dev.kolustar.ship.SableFrames.reliable(player)
                        && packet
                                instanceof
                                dev.ryanhcode.sable.network.packets
                                        .ClientboundSableSnapshotDualPacket
                ? packet
                : PayloadMap.relabel(player, packet);
    }
}
