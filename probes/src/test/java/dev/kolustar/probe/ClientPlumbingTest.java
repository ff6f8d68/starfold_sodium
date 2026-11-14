package dev.kolustar.probe;

import static org.junit.jupiter.api.Assertions.*;

import dev.kolustar.Sources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class ClientPlumbingTest {
    @Test
    void productionClientsNoLongerNameRenderOrDevelopment() throws Exception {
        for (String name :
                new String[] {"HullLandingClient", "SableCarryClient", "WaypointClient"}) {
            var source = Sources.text(name);
            assertFalse(source.contains("dev.kolustar.render"), name);
            assertFalse(source.contains("dev.kolustar.dev"), name);
        }
        for (String pkg : new String[] {"network", "render"}) {
            try (var files = Files.walk(Path.of("src/main/java/dev/kolustar/" + pkg))) {
                for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var source = Files.readString(file);
                    assertFalse(source.contains("dev.kolustar.dev."), file.toString());
                    if (!pkg.equals("render")) {
                        for (String higher :
                                new String[] {
                                    "sky",
                                    "worldgen",
                                    "universe",
                                    "ground",
                                    "flight",
                                    "ship",
                                    "space",
                                    "waypoint",
                                    "render"
                                })
                            assertFalse(
                                    source.contains("dev.kolustar." + higher + "."),
                                    file.toString());
                    }
                }
            }
        }
    }

}
