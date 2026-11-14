package dev.kolustar.probes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.kolustar.dev.DevMeshProbe;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
public abstract class MeshTaskProbeMixin implements DevMeshProbe.TaskAccess {
    @Unique private DevMeshProbe.Trace kolustar$trace;
    @Override public DevMeshProbe.Trace kolustar$trace() { return kolustar$trace; }
    @Override public void kolustar$trace(DevMeshProbe.Trace trace) { kolustar$trace = trace; }

    @Inject(method = "doTask", at = @At("HEAD"))
    private void kolustar$started(SectionBufferBuilderPack buffers, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (DevMeshProbe.active(kolustar$trace) && kolustar$trace.polled != 0)
            DevMeshProbe.EXECUTOR.add(System.nanoTime() - kolustar$trace.polled);
    }
    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"))
    private SectionCompiler.Results kolustar$compile(SectionCompiler compiler, SectionPos pos, RenderChunkRegion region,
            VertexSorting sorting, SectionBufferBuilderPack buffers, List<?> extra, Operation<SectionCompiler.Results> original) {
        long start = System.nanoTime();
        var result = original.call(compiler, pos, region, sorting, buffers, extra);
        kolustar$trace.built = System.nanoTime();
        if (DevMeshProbe.active(kolustar$trace)) DevMeshProbe.COMPILE.add(kolustar$trace.built - start);
        return result;
    }
    @WrapOperation(method = {"doTask", "lambda$doTask$1"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setCompiled(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;)V"))
    private void kolustar$published(SectionRenderDispatcher.RenderSection section,
            SectionRenderDispatcher.CompiledSection compiled, Operation<Void> original) {
        original.call(section, compiled);
        DevMeshProbe.published(kolustar$trace);
        ((DevMeshProbe.SectionAccess) section).kolustar$published(kolustar$trace);
    }
    @Inject(method = "cancel", at = @At("HEAD"))
    private void kolustar$cancelled(CallbackInfo ci) { DevMeshProbe.cancelled(kolustar$trace); }
}
