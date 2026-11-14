package dev.kolustar.dev;

import dev.kolustar.probe.ArrivalClientState;
import dev.kolustar.probe.ArrivalProbe;
import dev.kolustar.render.GroundRenderer;
import dev.kolustar.render.GroundViewClient;
import dev.kolustar.space.SpaceClient;

import net.minecraft.client.Minecraft;

/** Draw-call census, not a claim about camera occlusion or final pixels. */
public final class ArrivalClientProbe {
    private static long previous;

    public static void initialize() {
        ArrivalClientState.census(ArrivalClientProbe::end);
    }

    public static void end(long start) {
        if (!ArrivalProbe.ENABLED) return;
        var mc = Minecraft.getInstance();
        long now = System.nanoTime(), interval = previous == 0 ? 0 : now - previous;
        previous = now;
        if (mc.level == null || mc.player == null) return;
        boolean near = false;
        int x = mc.player.getBlockX() >> 4, z = mc.player.getBlockZ() >> 4;
        for (var section :
                ((dev.kolustar.mixin.GroundVisibleSectionsAccess) mc.levelRenderer)
                        .kolustar$visibleSections())
            if (section.getOrigin().getX() >> 4 == x
                    && section.getOrigin().getZ() >> 4 == z
                    && !section.getCompiled().hasNoRenderableLayers()) {
                near = true;
                break;
            }
        org.slf4j.LoggerFactory.getLogger("kolustar-probe")
                .info(
                        "PROBE ARRIVAL_FRAME wall {} game {} ns {} interval {} region {} remote {} own {} near {} missing {} parents {} coverage {} tiles {} parentNs {}",
                        System.currentTimeMillis(),
                        mc.level.getGameTime(),
                        now - start,
                        interval,
                        SpaceClient.active(),
                        dev.kolustar.DevelopmentHooks.remoteDraw,
                        dev.kolustar.DevelopmentHooks.ownDraw,
                        near,
                        !mc.level.getChunkSource().hasChunk(x, z),
                        GroundRenderer.probeParents(),
                        GroundViewClient.coverageReady(),
                        GroundRenderer.probeParentTiles(),
                        dev.kolustar.DevelopmentHooks.parentNanos);
    }

    private ArrivalClientProbe() {}
}
