package dev.kolustar.mixin;

import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FlatLevelGeneratorSettings.class)
public interface GroundFlatAccess {
    @Accessor("decoration")
    boolean kolustar$decoration();

    @Accessor("addLakes")
    boolean kolustar$lakes();
}
