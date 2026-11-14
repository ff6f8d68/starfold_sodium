package dev.kolustar.mixin;

import dev.ryanhcode.sable.Sable;
import dev.kolustar.ship.CloakMotion;

import net.minecraft.world.entity.player.Player;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every rendered player, not just the pilot. Off-hull and elytra flight use vanilla unchanged. */
@Mixin(Player.class)
public abstract class CloakMixin {
    @Unique private Object kolustar$body;
    @Unique private Vector3d kolustar$cloak, kolustar$feet;

    @Inject(method = "moveCloak", at = @At("HEAD"), cancellable = true)
    private void kolustar$relativeCloak(CallbackInfo ci) {
        var p = (Player) (Object) this;
        var body = Sable.HELPER.getTrackingSubLevel(p);
        if (!p.level().isClientSide || body == null || p.isFallFlying()) {
            kolustar$body = null;
            return;
        }
        var pose = body.logicalPose();
        var feet = pose.transformPositionInverse(new Vector3d(p.getX(), p.getY(), p.getZ()));
        if (kolustar$body != body) {
            kolustar$body = body;
            kolustar$cloak = new Vector3d(feet);
            kolustar$feet = new Vector3d(feet);
        }
        var old = pose.orientation().transform(new Vector3d(kolustar$cloak).sub(kolustar$feet));
        kolustar$cloak.set(
                CloakMotion.follow(kolustar$cloak.x, feet.x),
                CloakMotion.follow(kolustar$cloak.y, feet.y),
                CloakMotion.follow(kolustar$cloak.z, feet.z));
        var lag = pose.orientation().transform(new Vector3d(kolustar$cloak).sub(feet));
        p.xCloakO = p.xo + old.x;
        p.yCloakO = p.yo + old.y;
        p.zCloakO = p.zo + old.z;
        p.xCloak = p.getX() + lag.x;
        p.yCloak = p.getY() + lag.y;
        p.zCloak = p.getZ() + lag.z;
        kolustar$feet.set(feet);
        ci.cancel();
    }
}
