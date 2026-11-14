package dev.kolustar.probes.mixin;

import dev.kolustar.dev.DevMeshProbe;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class MeshSectionProbeMixin implements DevMeshProbe.SectionAccess {
    @Unique private long kolustar$generation, kolustar$dirtySince, kolustar$prepare;
    @Unique private volatile DevMeshProbe.Trace kolustar$published;

    @Inject(method = "setDirty", at = @At("HEAD"))
    private void kolustar$dirty(boolean player, CallbackInfo ci) {
        if (kolustar$dirtySince == 0) kolustar$dirtySince = System.nanoTime();
        kolustar$generation++;
        DevMeshProbe.dirty(player);
    }
    @Inject(method = "reset", at = @At("RETURN"))
    private void kolustar$reset(CallbackInfo ci) {
        kolustar$published = null;
        kolustar$dirtySince = System.nanoTime();
        kolustar$generation++;
    }
    @Inject(method = "createCompileTask", at = @At("HEAD"))
    private void kolustar$prepare(RenderRegionCache cache, CallbackInfoReturnable<Object> cir) {
        kolustar$prepare = System.nanoTime();
    }
    @Inject(method = "createCompileTask", at = @At("RETURN"))
    private void kolustar$created(RenderRegionCache cache, CallbackInfoReturnable<Object> cir) {
        var section = (SectionRenderDispatcher.RenderSection) (Object) this;
        var trace = new DevMeshProbe.Trace(section.getOrigin().asLong(), kolustar$generation);
        ((DevMeshProbe.TaskAccess) cir.getReturnValue()).kolustar$trace(trace);
        DevMeshProbe.created(trace, section.getCompiled() == SectionRenderDispatcher.CompiledSection.UNCOMPILED,
                trace.created - kolustar$prepare, kolustar$dirtySince);
        kolustar$dirtySince = 0;
    }
    @Override public void kolustar$published(DevMeshProbe.Trace trace) { kolustar$published = trace; }
    @Override public void kolustar$drawn() {
        var trace = kolustar$published;
        if (trace != null) {
            kolustar$published = null;
            DevMeshProbe.drawn(trace);
        }
    }
}
