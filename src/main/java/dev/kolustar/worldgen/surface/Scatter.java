package dev.kolustar.worldgen.surface;

import dev.kolustar.geometry.CubeSurfaceTopology.Cell;
import dev.kolustar.geometry.CubeSurfaceTopology.Face;
import dev.kolustar.worldgen.PlanetTerrain;

/** Shared canonical addressing and deterministic generation hash. */
public final class Scatter {
    static Cell canonical(PlanetTerrain t, Face face, int u, int v) {
        return t.shape().topology().canonical(new Cell(face, u, 0, v)).cell();
    }

    public static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    private Scatter() {}
}
