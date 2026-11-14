package dev.kolustar.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A path carried across a face seam keeps its speed and recomputation target. */
@Mixin(PathNavigation.class)
public interface GroundNavigationAccess {
    @Accessor("speedModifier")
    double kolustar$speed();

    @Accessor("targetPos")
    BlockPos kolustar$target();

    @Accessor("targetPos")
    void kolustar$target(BlockPos pos);
}
