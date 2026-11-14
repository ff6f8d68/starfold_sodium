package dev.kolustar.probes.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface GroundPositionDiagnosticAccess {
    @Accessor("awaitingTeleport")
    int kolustar$diagnosticTeleportId();

    @Accessor("awaitingPositionFromClient")
    Vec3 kolustar$diagnosticAwaiting();

    @Accessor("tickCount")
    int kolustar$diagnosticListenerTick();
}
