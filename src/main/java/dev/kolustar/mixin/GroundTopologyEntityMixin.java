package dev.kolustar.mixin;

import dev.kolustar.ground.GroundSeams;
import dev.kolustar.planet.PlanetLevels;

import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class GroundTopologyEntityMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 kolustar$sunWall(Vec3 motion) {
        return dev.kolustar.space.SpaceServer.sunMotion((Entity) (Object) this, motion);
    }

    @Inject(method = "move", at = @At("RETURN"))
    // A move made for a client (the player itself, or the vehicle it steers) is finished by its
    // network handler,
    // which goes on to compare with the client's coordinates: the chart changes after that, not
    // inside it.
    private void kolustar$nativeSeam(MoverType type, Vec3 delta, CallbackInfo ci) {
        if (type != MoverType.PLAYER
                && !((Object) this instanceof net.minecraft.server.level.ServerPlayer))
            GroundSeams.moved((Entity) (Object) this);
    }

    @Inject(method = "distanceToSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void kolustar$nativeDistance(
            double x, double y, double z, CallbackInfoReturnable<Double> ci) {
        var e = (Entity) (Object) this;
        if (PlanetLevels.canonical(e.level())) ci.setReturnValue(GroundSeams.distance(e, x, y, z));
    }

    @Inject(
            method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D",
            at = @At("HEAD"),
            cancellable = true)
    private void kolustar$nativeVectorDistance(Vec3 p, CallbackInfoReturnable<Double> ci) {
        var e = (Entity) (Object) this;
        if (PlanetLevels.canonical(e.level()))
            ci.setReturnValue(GroundSeams.distance(e, p.x, p.y, p.z));
    }

    @Inject(
            method = "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D",
            at = @At("HEAD"),
            cancellable = true)
    private void kolustar$nativeEntityDistance(Entity other, CallbackInfoReturnable<Double> ci) {
        var e = (Entity) (Object) this;
        if (PlanetLevels.canonical(e.level()))
            ci.setReturnValue(GroundSeams.distance(e, other.getX(), other.getY(), other.getZ()));
    }
}
