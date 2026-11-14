package dev.kolustar.mixin;

import dev.kolustar.ground.ChunkTickReadiness;

import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A seam chunk's generation leftovers wait for the chunks across the seam they read. */
@Mixin(LevelChunk.class)
public abstract class GroundPostProcessMixin {
    @Inject(method = "postProcessGeneration", at = @At("HEAD"), cancellable = true)
    private void kolustar$waitForSeam(CallbackInfo ci) {
        if (ChunkTickReadiness.holdPostProcess((LevelChunk) (Object) this)) ci.cancel();
    }
}
