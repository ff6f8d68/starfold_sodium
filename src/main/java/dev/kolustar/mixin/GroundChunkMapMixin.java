package dev.kolustar.mixin;

import dev.kolustar.flight.GroundFlightServer;
import dev.kolustar.ground.GroundTickets;
import dev.kolustar.planet.PlanetLevels;

import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ChunkMap.class)
public abstract class GroundChunkMapMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> entityMap;

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void kolustar$refresh(CallbackInfo ci) {
        if (PlanetLevels.is(level) && level.getGameTime() % 5 == 0)
            for (var tracker : entityMap.values())
                ((TrackedAccess) tracker).kolustar$updateObservers(level.players());
    }

    /** A vessel far above a planet loads none of it: its place over the ground sweeps whole faces as it passes, and what
     *  is seen from there is drawn from the far view. Chunks load again as it comes down to land. */
    @Inject(method = "skipPlayer", at = @At("HEAD"), cancellable = true)
    private void kolustar$aloft(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (PlanetLevels.is(level) && GroundFlightServer.aloft(player)) ci.setReturnValue(true);
    }

    @Inject(method = "anyPlayerCloseEnoughForSpawning", at = @At("HEAD"), cancellable = true)
    private void kolustar$aliasTick(ChunkPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (PlanetLevels.is(level) && GroundTickets.contains(level, pos)) ci.setReturnValue(true);
    }
}
