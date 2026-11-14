package dev.kolustar.probes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opt-in wall time includes chunk serialization, IO waits and mod save hooks. */
@Mixin(ServerLevel.class)
public abstract class ChunkPerfSaveMixin {
    @Unique private long kolustar$saveStart;

    @Inject(method = "save", at = @At("HEAD"))
    private void kolustar$saveStart(ProgressListener progress, boolean flush, boolean skip, CallbackInfo ci) {
        if (!Boolean.getBoolean("kolustar.chunkPerf")) return;
        kolustar$saveStart = System.nanoTime();
        org.slf4j.LoggerFactory.getLogger("kolustar-chunk-perf").info("PROBE PERF save-start dim {} flush {} skip {}",
                ((ServerLevel) (Object) this).dimension().location(), flush, skip);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void kolustar$saveEnd(ProgressListener progress, boolean flush, boolean skip, CallbackInfo ci) {
        if (!Boolean.getBoolean("kolustar.chunkPerf")) return;
        org.slf4j.LoggerFactory.getLogger("kolustar-chunk-perf").info("PROBE PERF save-end dim {} ms {}",
                ((ServerLevel) (Object) this).dimension().location(), (System.nanoTime() - kolustar$saveStart) / 1e6);
    }
}
