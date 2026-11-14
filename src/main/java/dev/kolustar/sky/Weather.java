package dev.kolustar.sky;

import dev.kolustar.chart.GroundOrbit;
import dev.kolustar.planet.GroundCoordinates;
import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Weather is still shared by the Level, but exists only below the top of the chart air. */
public final class Weather {
    static boolean aboveAir(double y) {
        return y - GroundCoordinates.FLOOR >= GroundOrbit.END;
    }

    /** Canonical storage coordinates; the client checks its atmosphere before converting to storage. */
    public static boolean absent(Level level, BlockPos pos) {
        return PlanetLevels.is(level)
                && (Space.holds(pos.getX(), pos.getZ())
                        || !level.isClientSide
                                && dev.kolustar.planet.PlanetConditions.airless(level, pos)
                        || aboveAir(pos.getY()) && PlanetAccess.of(level).inChart(pos));
    }

    private Weather() {}
}
