package dev.kolustar.mixin.hbm;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.kolustar.planet.PlanetLevels;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;

/**
 * HBM Continuum (GemRender) block-entity models can be culled incorrectly on Kolustar planets
 * because their render bounds/camera-space assumptions do not account for planet chart/projection
 * transforms. Keep them visible on planet levels to avoid models flickering out of existence.
 */
@Mixin(value = BlockEntityRenderDispatcher.class, priority = 1100)
public abstract class HbmPlanetRenderMixin {
    @WrapMethod(method = "shouldRender(Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z")
    private <T extends BlockEntity> boolean kolustar$keepHbmVisibleOnPlanet(
            BlockEntityRenderer<T> renderer,
            T blockEntity,
            Frustum frustum,
            double x,
            double y,
            double z,
            Operation<Boolean> original) {
        if (blockEntity != null
                && PlanetLevels.is(blockEntity.getLevel())
                && blockEntity
                        .getType()
                        .builtInRegistryHolder()
                        .key()
                        .location()
                        .getNamespace()
                        .equals("hbm")) {
            return true;
        }
        return original.call(renderer, blockEntity, frustum, x, y, z);
    }
}
