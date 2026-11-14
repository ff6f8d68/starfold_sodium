package dev.kolustar.mixin;

import dev.kolustar.ground.ChunkTickReadiness;
import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ghost chunks are display/query addresses, never a second random-tick/weather simulation. */
@Mixin(ServerLevel.class)
public abstract class GroundChunkTickMixin {
    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("RETURN"), cancellable = true)
    private void kolustar$readableNeighbours(
            long chunk,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ() && !ChunkTickReadiness.ready((ServerLevel) (Object) this, chunk))
            ci.setReturnValue(false);
    }

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void kolustar$onlyOwner(LevelChunk chunk, int speed, CallbackInfo ci) {
        if (!ChunkTickReadiness.ready((ServerLevel) (Object) this, chunk.getPos().toLong())) {
            ci.cancel();
            return;
        }
        if (PlanetLevels.is((ServerLevel) (Object) this)
                && PlanetAccess.of((ServerLevel) (Object) this)
                                .key(chunk.getPos().x, chunk.getPos().z)
                        < 0
                && PlanetAccess.of((ServerLevel) (Object) this)
                        .inChart(
                                new net.minecraft.core.BlockPos(
                                        chunk.getPos().getMiddleBlockX(),
                                        128,
                                        chunk.getPos().getMiddleBlockZ()))) ci.cancel();
    }
}
