package dev.kolustar.atlas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class MinimapPanelTest {
    @Test
    void panelStaysInsideTheViewportAtEveryWindowSize() {
        for (int width : new int[] {100, 320, 640, 1280, 3840})
            for (int height : new int[] {80, 240, 480, 720, 2160}) {
                int size = PlanetMinimap.size(width, height),
                        x = PlanetMinimap.originX(width, height),
                        y = PlanetMinimap.originY(width, height);
                assertTrue(size >= PlanetMinimap.MINIMUM, width + "x" + height);
                assertTrue(size <= PlanetMinimap.SIZE);
                assertTrue(x >= 0 && y >= 0, "negative panel origin");
                assertTrue(x + size <= width && y + size <= height,
                        "panel leaves the " + width + "x" + height + " viewport");
            }
    }

    @Test
    void compactPanelSuppressesChartFurniture() throws Exception {
        var view = Files.readString(Path.of("atlas/src/main/java/dev/kolustar/atlas/GroundView.java"));
        var render = view.substring(view.indexOf("public void render("),
                view.indexOf("private void terrain("));
        assertTrue(render.contains("if (!compact) anomalies(g)"));
        assertTrue(render.contains("if (!compact) labels.place("));
        assertEquals(3, view.split("if \\(compact\\) return;", -1).length - 1,
                "grid, readout and labels must each skip themselves");
        assertFalse(render.contains("Tesselator"));
        var panel = Files.readString(Path.of("atlas/src/main/java/dev/kolustar/atlas/PlanetMinimap.java"));
        assertTrue(panel.contains("view.compact(true)"));
        assertTrue(panel.contains("view.northUp(true)"));
        // The panel is a HUD overlay, so it must never open a screen of its own.
        assertFalse(panel.contains("setScreen("));
    }

    @Test
    void worldMapKeyOpensTheWrappingChartLayer() throws Exception {
        var screen = Files.readString(Path.of("atlas/src/main/java/dev/kolustar/atlas/AtlasScreen.java"));
        assertTrue(screen.contains("if (initial == Layer.GROUND && ground != null) layer(Layer.GROUND)"));
        var atlas = Files.readString(Path.of("atlas/src/main/java/dev/kolustar/atlas/Atlas.java"));
        assertTrue(atlas.contains("new AtlasScreen(AtlasScreen.Layer.GROUND)"));
        assertTrue(atlas.contains("PlanetMinimap.render(event.getGuiGraphics())"));
    }
}
