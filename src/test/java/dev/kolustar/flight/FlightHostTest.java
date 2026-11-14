package dev.kolustar.flight;

import static org.junit.jupiter.api.Assertions.*;

import dev.kolustar.Sources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class FlightHostTest {
    @Test
    void personalFlightHasNoUpperLayerReferences() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/dev/kolustar/flight"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(
                        java.util.regex.Pattern.compile(
                                        "dev\\.kolustar\\.(ship|space|waypoint|render)")
                                .matcher(Files.readString(file))
                                .find(),
                        file.toString());
            }
        }
    }

    @Test
    void clientRequestsKeepTheirOriginalDelegates() throws Exception {
        var wiring = Sources.text("KolustarClient");
        assertTrue(wiring.contains("FlightClientHost.install(new FlightClientHost()"));
        for (String delegate :
                List.of(
                        "void departing(){PlanetRemoteRenderer.departing();}",
                        "void sessionBound(ClientLevel level,long session){GroundViewClient.bindSession(level,session);}",
                        "void atmosphere(GroundAir.Config config){GroundClient.air(config);}",
                        "void metadata(GroundEphemeris.Model model,long tick){GroundClient.metadata(model,tick);}",
                        "boolean viewReady(){return GroundClient.ready();}",
                        "boolean diagnosticHud(){return GroundClient.DEBUG_HUD;}",
                        "Point chartPosition(int planet,PlanetShape.Pose pose){return GroundClient.plane(planet,pose);}",
                        "int chartTurns(Face face){return GroundClient.planeTurns(face);}",
                        "Point chartPoint(int planet,Face face,Point point){return GroundClient.planePoint(planet,face,point);}"))
            assertTrue(wiring.contains(delegate), delegate);
    }
}
