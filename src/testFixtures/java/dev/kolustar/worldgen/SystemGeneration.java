package dev.kolustar.worldgen;

import dev.kolustar.universe.PlanetChunkGenerator;
import dev.kolustar.universe.SystemChunkGenerator;
import dev.kolustar.planet.PlanetStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Test fixtures must use the registered generator even when they hold one planet. */
public final class SystemGeneration {
    public static CompletableFuture<ChunkAccess> fill(ChunkGenerator generator, ChunkAccess chunk) {
        var system = generator instanceof PlanetChunkGenerator planet
                ? new SystemChunkGenerator(PlanetStorage.LAYOUT, List.of(planet)) : generator;
        return system.fillFromNoise(null, null, null, chunk);
    }
    private SystemGeneration() {}
}
