package dev.kolustar.mixin.hbm;

import dev.kolustar.planet.PlanetLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Golem Core's GemRender culls whole machines and individual model parts against the learned
 * model bounds ({@code dev.golem.core.render.Frustums.isVisible}). Those bounds are learned in
 * straight world space and assume the flat vanilla chart, so on a curved Kolustar planet parts
 * drop out and reappear as the camera turns. The renderer still decides what to draw; only the
 * Golem frustum test is skipped on planet levels.
 */
@Mixin(value = dev.golem.core.render.Frustums.class, remap = false)
public abstract class HbmFrustumPlanetMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private static void kolustar$visibleOnPlanets(AABB box, CallbackInfoReturnable<Boolean> ci) {
        var mc = Minecraft.getInstance();
        if (mc != null && PlanetLevels.is(mc.level)) ci.setReturnValue(true);
    }
}
