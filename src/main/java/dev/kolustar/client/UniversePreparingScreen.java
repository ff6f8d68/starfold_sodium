package dev.kolustar.client;

import dev.kolustar.worldgen.plan.PlanetPlanProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UniversePreparingScreen extends Screen {
    public UniversePreparingScreen() {
        super(Component.literal("Preparing universe..."));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        var progress = PlanetPlanProgress.get();
        String stage = progress == null ? "Preparing universe..." : progress.stage();
        int pct = progress == null ? -1 : progress.percent();
        int y = this.height / 2 - 20;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(stage), this.width / 2, y + 14, 0xAAAAAA);
        if (pct >= 0) {
            int barW = 200, barH = 6, x = this.width / 2 - barW / 2, by = y + 32;
            graphics.fill(x - 1, by - 1, x + barW + 1, by + barH + 1, 0xFF555555);
            graphics.fill(x, by, x + barW, by + barH, 0xFF222222);
            int fill = barW * pct / 100;
            if (fill > 0) graphics.fill(x, by, x + fill, by + barH, 0xFF55AAFF);
            graphics.drawCenteredString(this.font, Component.literal(pct + "%"), this.width / 2, by + 10, 0xCCCCCC);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected boolean shouldNarrateNavigation() { return true; }

    public static void tickClient() {
        Minecraft mc = Minecraft.getInstance();
        var progress = PlanetPlanProgress.get();
        if (progress == null) {
            if (mc.screen instanceof UniversePreparingScreen) mc.setScreen(null);
            return;
        }
        if (!(mc.screen instanceof UniversePreparingScreen)) {
            // Don't interrupt critical screens like disconnect/error; show during gameplay/loading context.
            if (mc.screen == null || mc.screen.getClass().getName().contains("Pause") || mc.screen.getClass().getName().contains("Chat"))
                mc.setScreen(new UniversePreparingScreen());
            else if (mc.level == null)
                mc.setScreen(new UniversePreparingScreen());
        }
    }
}
