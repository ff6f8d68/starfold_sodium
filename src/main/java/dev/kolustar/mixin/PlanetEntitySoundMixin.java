package dev.kolustar.mixin;

import dev.kolustar.planet.PlanetSoundOwner;

import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityBoundSoundInstance.class)
public abstract class PlanetEntitySoundMixin implements PlanetSoundOwner {
    @Shadow @Final private Entity entity;

    @Override
    public Object kolustar$soundOwner() {
        return entity;
    }
}
