package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.kolustar.ground.LightSeams;

import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Block light propagation across face seams (see GroundLightSeamMixin). */
@Mixin(BlockLightEngine.class)
public abstract class GroundBlockLightSeamMixin {
    @WrapOperation(
            method = {"checkNode", "propagateIncrease", "propagateDecrease"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;getStoredLevel(J)I"))
    private int kolustar$get(
            BlockLightSectionStorage storage, long pos, Operation<Integer> original) {
        return LightSeams.get(this, storage, pos, owner -> original.call(storage, owner));
    }

    @WrapOperation(
            method = {"checkNode", "propagateIncrease", "propagateDecrease"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;setStoredLevel(JI)V"))
    private void kolustar$set(
            BlockLightSectionStorage storage, long pos, int level, Operation<Void> original) {
        LightSeams.set(this, storage, pos, level, owner -> original.call(storage, owner, level));
    }

    @WrapOperation(
            method = {"checkNode", "propagateIncrease", "propagateDecrease"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;storingLightForSection(J)Z"))
    private boolean kolustar$stored(
            BlockLightSectionStorage storage, long section, Operation<Boolean> original) {
        return original.call(storage, LightSeams.section(this, section));
    }

    @WrapOperation(
            method = "getEmission",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;lightOnInSection(J)Z"))
    private boolean kolustar$on(
            BlockLightSectionStorage storage, long section, Operation<Boolean> original) {
        return original.call(storage, LightSeams.section(this, section));
    }
}
