package dev.kolustar.mixin;

import dev.kolustar.render.ReversedDepth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/**
 * Veil (bundled with Aeronautics and also installed standalone) clears its framebuffers, the
 * first-person hand target among them, with raw GL. Only the classes that exist across Veil
 * 4.3.x-4.5.x are targeted: the older wrapper package is gone, and listing it made the whole
 * depth patch fail to apply. Veil's TextureTarget wrapper inherits the RenderTarget clear that is
 * already handled, so it needs no target of its own.
 */
@Mixin(
        targets = {
            "foundry.veil.impl.client.render.framebuffer.DSAAdvancedFboImpl",
            "foundry.veil.impl.client.render.framebuffer.LegacyAdvancedFboImpl"
        },
        remap = false)
public abstract class ReversedDepthVeilMixin {
    @ModifyVariable(method = "clear(FFFFFI[I)V", at = @At("HEAD"), argsOnly = true, ordinal = 4, require = 0)
    private float kolustar$clearDepth(float depth) {
        return ReversedDepth.ACTIVE ? 1 - depth : depth;
    }
}
