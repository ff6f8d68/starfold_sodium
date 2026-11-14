package dev.kolustar.mixin.hbm;

import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Keep HBM radar sampling on the local planet chart so curved/wrapped planet storage does not
 *  produce invalid chunk/height reads or cross-planet entity matches. */
@Mixin(value = TileEntityMachineRadarNT.class, remap = false)
public abstract class HbmRadarPlanetMixin {
    @Shadow public BlockPos origin;

    private boolean kolustar$planet(Level level) {
        return PlanetLevels.is(level);
    }

    private boolean kolustar$validSample(Level level, int x, int z) {
        if (!kolustar$planet(level) || origin == null) return true;
        BlockPos here = new BlockPos(origin.getX(), origin.getY(), origin.getZ());
        BlockPos sample = new BlockPos(x, origin.getY(), z);
        return PlanetAccess.of(level).samePlanet(here.getX(), here.getZ(), sample.getX(), sample.getZ())
                && PlanetAccess.of(level).inChart(sample);
    }

    @WrapMethod(method = "sampleSlot")
    private void kolustar$guardSample(ServerLevel level, int slot, Operation<Void> original) {
        if (!kolustar$planet(level) || origin == null) {
            original.call(level, slot);
            return;
        }
        int range = ((TileEntityMachineRadarNT) (Object) this).getRange();
        // Skip slots whose sampled area leaves the local planet chart/storage to avoid invalid
        // chunk reads and height lookups on curved/wrapped planet coordinates.
        for (int i = 0; i < 100; i++) {
            int index = slot * 100 + i;
            int x = origin.getX() - range + (index % 200) * range * 2 / 200;
            int z = origin.getZ() - range + (index / 200) * range * 2 / 200;
            if (!kolustar$validSample(level, x, z)) {
                return;
            }
        }
        original.call(level, slot);
    }

    @WrapMethod(method = "prefetchSlot")
    private void kolustar$guardPrefetch(ServerLevel level, int slot, Operation<Void> original) {
        if (!kolustar$planet(level) || origin == null) {
            original.call(level, slot);
            return;
        }
        int range = ((TileEntityMachineRadarNT) (Object) this).getRange();
        for (int i = 0; i < 100; i++) {
            int index = slot * 100 + i;
            int x = origin.getX() - range + (index % 200) * range * 2 / 200;
            int z = origin.getZ() - range + (index / 200) * range * 2 / 200;
            if (!kolustar$validSample(level, x, z)) {
                return;
            }
        }
        original.call(level, slot);
    }

    @WrapMethod(method = "allocateTargets")
    private void kolustar$guardTargets(ServerLevel level, Operation<Void> original) {
        if (!kolustar$planet(level)) {
            original.call(level);
            return;
        }
        // Filter matching entities to the radar's planet storage/chart before HBM's flat scan.
        // matchingEntities is a shared static list in HBM; we temporarily filter in-place via
        // a copy is not possible here without reflection, so rely on per-entry checks in a
        // follow-up pass: run original and then remove entries not on the same planet/chart.
        original.call(level);
        var entries = ((TileEntityMachineRadarNT) (Object) this).entries;
        if (entries == null || origin == null) return;
        entries.removeIf(entry -> {
            try {
                // RadarEntry stores posX/posY/posZ fields; keep only samples on same planet.
                // Reflectively tolerant: if fields differ, keep entry.
                var fx = entry.getClass().getField("posX");
                var fz = entry.getClass().getField("posZ");
                int ex = ((Number) fx.get(entry)).intValue();
                int ez = ((Number) fz.get(entry)).intValue();
                BlockPos ep = new BlockPos(ex, origin.getY(), ez);
                return !PlanetAccess.of(level).samePlanet(origin.getX(), origin.getZ(), ep.getX(), ep.getZ());
            } catch (Throwable ignored) {
                return false;
            }
        });
    }
}
