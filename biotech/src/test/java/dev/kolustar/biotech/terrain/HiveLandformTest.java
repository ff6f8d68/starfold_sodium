package dev.kolustar.biotech.terrain;

import static org.junit.jupiter.api.Assertions.*;
import static dev.kolustar.worldgen.plan.terrain.TerrainPlan.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.geometry.CubeSurfaceTopology.Face;
import dev.kolustar.worldgen.plan.PlanetPlan;
import dev.kolustar.worldgen.plan.terrain.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.*;

class HiveLandformTest {
    private static com.google.gson.JsonObject definition() throws Exception {
        try (var in = HiveLandformTest.class.getResourceAsStream("/data/kolustar_biotech/kolustar/planet_type/flesh.json")) {
            return JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
        }
    }
    @BeforeAll static void boot() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test
    void hiveHasSphericalVoronoiWallsAndSavedFeaturesCannotInvadeAnomalies() throws Exception {
        var definition =
            definition();
        var recipe = TerrainRecipe.CODEC.parse(JsonOps.INSTANCE, definition.get("terrain")).getOrThrow();
        var plan = new TerrainPlanner(42, 512, recipe, 70).generate();
        assertFalse(plan.landforms().isEmpty());
        var form = plan.landforms().getFirst();
        assertTrue(form.parameters().getAsJsonArray("neighbours").size() >= 5);
        var center = form.center();
        var tangent = center.cross(Math.abs(center.y()) < .8 ? Face.TOP.normal : Face.EAST.normal);
        tangent = tangent.scale(1 / tangent.length());
        var other = center.cross(tangent);
        double angularVariation = 0;
        var noise = new dev.kolustar.worldgen.noise.TerrainNoise(42);
        for (double radius = .05; radius <= 1; radius += .05) {
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 64; i++) {
                double angle = i * Math.PI / 32;
                var p = center.add(tangent.scale(radius * Math.cos(angle)))
                            .add(other.scale(radius * Math.sin(angle)));
                p = p.scale(1 / p.length());
                double height = TerrainRegistries.LANDFORMS.get(form.type()).implementation()
                                    .evaluate(p, form, 512, noise) * form.amplitude();
                low = Math.min(low, height);
                high = Math.max(high, height);
            }
            angularVariation = Math.max(angularVariation, high - low);
        }
        assertTrue(angularVariation > 1,
            "Voronoi walls vary with angle on at least one circle; radial rings cannot pass");
        var fields = new float[6][plan.nodeCount()];
        var biomes = new short[plan.nodeCount()];
        var water = new byte[plan.nodeCount()];
        var corner = new Point(1, 1, 1).scale(1 / Math.sqrt(3));
        var invading = new Landform(
            form.type(), form.parameters(), corner, form.radius(), form.amplitude(), form.operation());
        var invalid = new TerrainPlan(42, 512, recipe, fields, biomes, water, List.of(invading), List.of());
        assertThrows(IllegalArgumentException.class,
            () -> PlanetPlan.decode(new PlanetPlan(512, invalid, List.of(), List.of()).compressed()));
    }

    @Test void fixedSeedOutputsMatchPreMoveBaseline() throws Exception {
        var definition = definition();
        var recipe = TerrainRecipe.CODEC.parse(JsonOps.INSTANCE, definition.get("terrain")).getOrThrow();
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        for (long seed : new long[] {42, 918273}) {
            var plan = new TerrainPlanner(seed, 512, recipe, 70).generate();
            byte[] bytes = new PlanetPlan(512, plan, List.of(), List.of()).compressed();
            // Refreshed by the kolustar rename: the archived plan differs only by id namespace.
            assertEquals(seed == 42
                    ? "9740a7fc4cd84d634e157eae890bef8fd009603a0dba8367f51b4997eb17de44"
                    : "a28cac49016a9d670532b5bcc6783b785a8d77191eb92212a0138a493a7d03a0",
                    HexFormat.of().formatHex(digest.digest(bytes)));
            var samples = new StringBuilder();
            for (var form : plan.landforms()) {
                samples.append(form.center()).append(form.parameters()).append(form.radius()).append(form.amplitude());
                var noise = new dev.kolustar.worldgen.noise.TerrainNoise(seed);
                for (var face : Face.VALUES)
                    samples.append(TerrainRegistries.LANDFORMS.get(form.type()).implementation().evaluate(face.normal, form, 512, noise));
            }
            assertEquals(seed == 42
                    ? "103421aa848e952dcfffff02beb9fa330af0f56e58bd6f9de28e5537fb92f5f5"
                    : "28339647b862fc7419fe8b0c8ba307f49f78f465f0dca8d5a0519d052a358d8f",
                    HexFormat.of().formatHex(digest.digest(samples.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        }
    }
}
