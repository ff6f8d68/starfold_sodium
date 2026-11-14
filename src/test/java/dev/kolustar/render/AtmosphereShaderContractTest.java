package dev.kolustar.render;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;

import dev.kolustar.sky.AtmosphereSettings;

import io.github.ocelot.glslprocessor.api.GlslParser;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** The sky dome integrates scattering on the GPU; its constants and uniforms must match the CPU twin. */
class AtmosphereShaderContractTest {
    private static final Path ROOT =
            Path.of("src/main/resources/assets/kolustar/shaders/core");

    private static Map<String, Double> defines(String source) {
        var out = new HashMap<String, Double>();
        for (var line : source.split("\n")) {
            var trimmed = line.strip();
            if (!trimmed.startsWith("#define SCATTER_")) continue;
            var parts = trimmed.split("\\s+");
            assertEquals(3, parts.length, trimmed);
            out.put(parts[1], Double.parseDouble(parts[2]));
        }
        return out;
    }

    @Test
    void scatteringConstantsMatchTheCpuIntegrator() throws Exception {
        var shader = Files.readString(ROOT.resolve("atmosphere_color.vsh"));
        var defines = defines(shader);
        var expected =
                Map.ofEntries(
                        Map.entry("SCATTER_VIEW_SAMPLES", (double) AtmosphereSettings.VIEW_SAMPLES),
                        Map.entry("SCATTER_SUN_SAMPLES", (double) AtmosphereSettings.SUN_SAMPLES),
                        Map.entry("SCATTER_SCALE_HEIGHT", AtmosphereSettings.SCALE_HEIGHT),
                        Map.entry("SCATTER_SHELL_HEIGHTS", AtmosphereSettings.SHELL_HEIGHTS),
                        Map.entry("SCATTER_RAYLEIGH", AtmosphereSettings.RAYLEIGH),
                        Map.entry("SCATTER_MIE", AtmosphereSettings.MIE),
                        Map.entry("SCATTER_MIE_G", AtmosphereSettings.MIE_G),
                        Map.entry("SCATTER_EXPOSURE", AtmosphereSettings.EXPOSURE),
                        Map.entry("SCATTER_TINT_POWER", AtmosphereSettings.TINT_POWER),
                        Map.entry("SCATTER_PI", Math.PI));
        assertEquals(new TreeSet<>(expected.keySet()), new TreeSet<>(defines.keySet()));
        expected.forEach((name, value) -> assertEquals(value, defines.get(name), 1e-12, name));
    }

    @Test
    void everyScatteringUniformIsDeclaredInTheShaderAndTheJson() throws Exception {
        var shader = Files.readString(ROOT.resolve("atmosphere_color.vsh"));
        var json =
                JsonParser.parseString(
                                Files.readString(ROOT.resolve("atmosphere_color.json")))
                        .getAsJsonObject();
        var declared = new HashMap<String, Integer>();
        for (var entry : json.getAsJsonArray("uniforms")) {
            var uniform = entry.getAsJsonObject();
            declared.put(uniform.get("name").getAsString(), uniform.get("count").getAsInt());
        }
        assertEquals(1, declared.get("SkyScatter"));
        assertEquals(1, declared.get("SkyRadius"));
        assertEquals(1, declared.get("SkyDensity"));
        assertEquals(1, declared.get("SkyTransmission"));
        assertEquals(1, declared.get("SkyScale"));
        assertEquals(3, declared.get("SkyEye"));
        assertEquals(3, declared.get("SkySun"));
        assertEquals(3, declared.get("SkyTint"));
        for (var name : List.of("SkyScatter", "SkyRadius", "SkyDensity", "SkyTransmission",
                "SkyScale"))
            assertTrue(shader.contains("uniform float " + name) || shader.contains(", " + name)
                    || shader.contains(" " + name + ",") || shader.contains(name + ";"), name);
        assertTrue(shader.contains("uniform vec3 SkyEye, SkySun, SkyTint;"));
        // The dome must not silently fall back to CPU-baked colours.
        assertTrue(shader.contains("SkyScatter>0.5?vec4(scatterColor(Position)*SkyScale,1.0):Color;"));
        assertNotNull(GlslParser.preprocessParse(shader, new HashMap<>()));
    }
}
