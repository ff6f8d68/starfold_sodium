package dev.kolustar.api;

import com.mojang.serialization.MapCodec;
import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.universe.PlanetSpawn;
import dev.kolustar.universe.SystemChunkGenerator;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;

/** Optional preset content; the core has ordinary terrain spawns and no landing pads. */
public interface PlanetContent {
    final class Registration {
        private static PlanetContent content = new PlanetContent() {};
        private Registration() {}
    }

    static PlanetContent get() { return Registration.content; }
    static void install(PlanetContent content) { Registration.content = content; }

    record Spawn(Point position, float yaw) {}

    default Spawn home(ServerLevel level, int face) {
        return new Spawn(PlanetSpawn.site(level, 0, face), 0);
    }

    default boolean startsWithShip(SystemChunkGenerator generator) { return false; }
    default boolean missingStarterDependencies() { return false; }
    default boolean overPad(ServerLevel level, ServerSubLevel body) { return false; }
    default Point launchCentre(ServerLevel level, ServerSubLevel body) { return null; }
    default MapCodec<SystemChunkGenerator> generatorCodec(MapCodec<SystemChunkGenerator> core) { return core; }
}
