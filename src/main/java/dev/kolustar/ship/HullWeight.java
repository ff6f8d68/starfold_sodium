package dev.kolustar.ship;

import dev.kolustar.chart.GroundEphemeris;
import dev.kolustar.chart.GroundGap;
import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.planet.PlanetAccess;
import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.sky.GroundClock;
import dev.kolustar.sky.Space;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Hull weight fades between the body's full-weight and weightless heights. Players keep their
 * weight at any height; space-region hulls remain weightless. */
public final class HullWeight {
    public static double at(GroundEphemeris.Body body, double height) {
        return 1 - GroundGap.smooth(body.weightFull(), body.weightNone(), height);
    }

    public static double at(Level level, Point feet) {
        if (Space.holds(feet.x(), feet.z())) return 0;
        var access = PlanetAccess.of(level);
        if (!PlanetLevels.is(level)
                || !access.inChart(BlockPos.containing(feet.x(), feet.y(), feet.z()))) return 1;
        var model = GroundClock.get((ServerLevel) level).clock.model;
        int planet = access.storagePlanet(feet.x(), feet.z());
        return at(model.body(planet), access.chart(feet).y());
    }

    /** Actual weight relative to vanilla dimension gravity, including the planet's surface factor. */
    public static double gravity(Level level, Point feet) {
        return at(level, feet)
                * dev.kolustar.planet.PlanetConditions.gravity(
                        level, BlockPos.containing(feet.x(), feet.y(), feet.z()));
    }

    private HullWeight() {}
}
