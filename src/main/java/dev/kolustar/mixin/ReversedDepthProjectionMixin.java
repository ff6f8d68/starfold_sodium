package dev.kolustar.mixin;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.kolustar.render.ReversedDepth;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class ReversedDepthProjectionMixin {
    @Inject(method = "initRenderer(IZ)V", at = @At("RETURN"))
    private static void kolustar$depthInit(int debug, boolean sync, CallbackInfo ci) {
        ReversedDepth.initialize();
    }

    @ModifyVariable(
            method = "setProjectionMatrix(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexSorting;)V",
            at = @At("HEAD"), argsOnly = true)
    private static Matrix4f kolustar$projection(Matrix4f matrix) {
        return ReversedDepth.ACTIVE ? ReversedDepth.projection(matrix) : matrix;
    }
}
