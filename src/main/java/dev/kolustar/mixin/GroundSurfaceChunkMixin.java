package dev.kolustar.mixin;

import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.render.GroundSurfaceIndex;
import dev.kolustar.render.GroundViewServer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Direct native chunk edits invalidate derived coverage too (not only Level.setBlock callers). */
@Mixin(LevelChunk.class)
public abstract class GroundSurfaceChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void kolustar$nativeChanged(
            BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> ci) {
        var chunk = (LevelChunk) (Object) this;
        if (ci.getReturnValue() != null && chunk.getLevel() instanceof ServerLevel level) {
            dev.kolustar.ship.Thrusters.changed(level, pos);
            if (!PlanetLevels.is(level)) return;
            if (PlanetAccess.of(level).key(chunk.getPos().x, chunk.getPos().z) >= 0)
                GroundSurfaceIndex.at(level, chunk.getPos().x, chunk.getPos().z).changed(chunk,pos);
            GroundViewServer.changed(level, pos, state);
        }
    }
}
