package dev.kolustar.mixin;

import dev.kolustar.ground.PacketMap;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Handlers run twice (network thread, then rescheduled on the server thread): map once, on the server thread. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GroundInboundMixin {
    @Shadow public ServerPlayer player;

    @Unique
    private boolean kolustar$server() {
        return player.getServer().isSameThread();
    }

    @ModifyVariable(method = "handleMovePlayer", at = @At("HEAD"), argsOnly = true)
    private ServerboundMovePlayerPacket kolustar$nativeMoveFrame(
            ServerboundMovePlayerPacket packet) {
        if (!kolustar$server()) return packet;
        var mapped = (ServerboundMovePlayerPacket) PacketMap.inbound(player, packet);
        dev.kolustar.DevelopmentHooks.mappedMove.accept(player, packet, mapped);
        return mapped;
    }

    @ModifyVariable(method = "handleMoveVehicle", at = @At("HEAD"), argsOnly = true)
    private ServerboundMoveVehiclePacket kolustar$nativeVehicleFrame(
            ServerboundMoveVehiclePacket packet) {
        return kolustar$server()
                ? (ServerboundMoveVehiclePacket) PacketMap.inbound(player, packet)
                : packet;
    }

    @ModifyVariable(method = "handleUseItemOn", at = @At("HEAD"), argsOnly = true)
    private ServerboundUseItemOnPacket kolustar$nativeUseOnFrame(
            ServerboundUseItemOnPacket packet) {
        return kolustar$server()
                ? (ServerboundUseItemOnPacket) PacketMap.inbound(player, packet)
                : packet;
    }

    @ModifyVariable(method = "handlePlayerAction", at = @At("HEAD"), argsOnly = true)
    private ServerboundPlayerActionPacket kolustar$nativeActionFrame(
            ServerboundPlayerActionPacket packet) {
        return kolustar$server()
                ? (ServerboundPlayerActionPacket) PacketMap.inbound(player, packet)
                : packet;
    }

    @ModifyVariable(method = "handleUseItem", at = @At("HEAD"), argsOnly = true)
    private ServerboundUseItemPacket kolustar$nativeUseFrame(ServerboundUseItemPacket packet) {
        return kolustar$server()
                ? (ServerboundUseItemPacket) PacketMap.inbound(player, packet)
                : packet;
    }

    @ModifyVariable(method = "handleSignUpdate", at = @At("HEAD"), argsOnly = true)
    private ServerboundSignUpdatePacket kolustar$nativeSignFrame(
            ServerboundSignUpdatePacket packet) {
        return kolustar$server()
                ? (ServerboundSignUpdatePacket) PacketMap.inbound(player, packet)
                : packet;
    }
}
