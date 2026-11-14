package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.kolustar.flight.GroundFlightServer;
import dev.kolustar.ground.GroundCollisions;
import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;

/** Vanilla movement's block-collision provider, not a replacement physics integrator. */
@Mixin(CollisionGetter.class)
public interface GroundTopologyCollisionMixin {
    @WrapMethod(method = "getBlockCollisions")
    private Iterable<VoxelShape> kolustar$nativeCollision(
            Entity entity, AABB box, Operation<Iterable<VoxelShape>> original) {
        if ((Object) this instanceof Level level && PlanetLevels.is(level)) {
            if (level.isClientSide) return new GroundCollisions(level, entity, box);
            if (PlanetAccess.of(level)
                    .inChart(net.minecraft.core.BlockPos.containing(box.getCenter())))
                return new GroundCollisions(level, entity, box, GroundFlightServer.mask(level));
        }
        return original.call(entity, box);
    }
}
