package dev.kolustar.atlas;

import dev.kolustar.api.PlanetMapApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Corner minimap over the same wrapping ground chart the atlas draws. It is a HUD panel, not a
 * screen: the panel follows the player north-up and shows waypoint bearings across cube edges
 * because the chart, not a flat world texture, supplies the pixels.
 */
public final class PlanetMinimap {
    static final int SIZE = 96, MARGIN = 6, MINIMUM = 32;
    /** Blocks across the panel: a coarse regional view, not a chat-range radar. */
    private static final double BLOCKS = 192;
    private static final int BORDER = 0xff71dce0;
    private static GroundView view;
    private static PlanetMapApi.Body body;

    /** Square panel side for a GUI-scaled viewport; small windows get a smaller panel, never none. */
    static int size(int guiWidth, int guiHeight) {
        return Math.max(MINIMUM, Math.min(SIZE, Math.min(guiWidth, guiHeight) / 3));
    }

    static int originX(int guiWidth, int guiHeight) {
        return Math.max(MARGIN, guiWidth - MARGIN - size(guiWidth, guiHeight));
    }

    static int originY(int guiWidth, int guiHeight) {
        return MARGIN;
    }

    static void render(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;
        var observer = PlanetMapApi.observer();
        if (observer == null || observer.inSpace()) {
            close();
            return;
        }
        if (body == null || body.index() != observer.body()) open(observer.body());
        if (view == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth(),
                height = minecraft.getWindow().getGuiScaledHeight();
        int size = size(width, height), x = originX(width, height), y = originY(width, height);
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, BORDER);
        view.render(graphics, x, y, size, size, 0);
    }

    private static void open(int index) {
        close();
        var next =
                PlanetMapApi.bodies().stream()
                        .filter(candidate -> candidate.index() == index && candidate.surface())
                        .findFirst()
                        .orElse(null);
        if (next == null) return;
        body = next;
        view = new GroundView(next);
        view.compact(true);
        view.northUp(true);
        view.scale(SIZE / BLOCKS);
    }

    /** Releases the panel's textures; the overlay reopens on the next planet frame. */
    static void close() {
        if (view != null) view.close();
        view = null;
        body = null;
    }

    private PlanetMinimap() {}
}
