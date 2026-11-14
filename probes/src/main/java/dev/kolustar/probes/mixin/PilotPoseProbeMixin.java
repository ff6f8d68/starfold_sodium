package dev.kolustar.probes.mixin;

import dev.kolustar.dev.PilotPoseProbe;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read the actual fit test, including Sable's standing-box collision wrapper, on both sides. */
@Mixin(Player.class)
public abstract class PilotPoseProbeMixin {
    @Shadow
    protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

    @Inject(method = "updatePlayerPose", at = @At("RETURN"))
    private void kolustar$poseProbe(CallbackInfo ci) {
        if (!Boolean.getBoolean("kolustar.proneProbe")
                && !Boolean.getBoolean("kolustar.carryProbe")
                && System.getProperty("kolustar.probe") == null) return;
        var player = (Player) (Object) this;
        boolean fits = canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING);
        if (Boolean.getBoolean("kolustar.proneProbe")) {
            var previous = dev.kolustar.ship.RiderPoseContext.BASELINE.get();
            boolean before;
            dev.kolustar.ship.RiderPoseContext.BASELINE.set(true);
            try {
                before = canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING);
            } finally {
                if (previous == null) dev.kolustar.ship.RiderPoseContext.BASELINE.remove();
                else dev.kolustar.ship.RiderPoseContext.BASELINE.set(previous);
            }
            org.slf4j.LoggerFactory.getLogger("kolustar-probe")
                    .info(
                            "PROBE FITCOMPARE side {} tick {} pose {} tracked {} liveHelm {} before {} after {} swimming {} sneak {} ground {} uuid {}",
                            player.level().isClientSide ? "client" : "server",
                            player.level().getGameTime(),
                            player.getPose(),
                            dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(player) != null,
                            dev.kolustar.ship.RiderPoseContext.liveHelm(player),
                            before,
                            fits,
                            player.isSwimming(),
                            player.isShiftKeyDown(),
                            player.onGround(),
                            player.getUUID());
        }
        PilotPoseProbe.sample(player, fits);
    }
}
