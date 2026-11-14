package dev.kolustar.atlas;

import dev.kolustar.api.SystemMapApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@Mod(value = "kolustar_atlas", dist = Dist.CLIENT)
public final class Atlas {
    static final SurveyStore surveys = new SurveyStore();
    private static final KeyMapping MAP = new KeyMapping("key.kolustar_atlas.map", GLFW.GLFW_KEY_M, "key.categories.kolustar_atlas");
    private static final KeyMapping GROUND = new KeyMapping("key.kolustar_atlas.ground_map", GLFW.GLFW_KEY_K, "key.categories.kolustar_atlas");
    private static final KeyMapping MINIMAP = new KeyMapping("key.kolustar_atlas.minimap", GLFW.GLFW_KEY_J, "key.categories.kolustar_atlas");
    public Atlas(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(MAP);
            event.register(GROUND);
            event.register(MINIMAP);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            surveys.tick();
            var mc = Minecraft.getInstance();
            while (MAP.consumeClick()) if (mc.player != null && mc.screen == null) {
                mc.setScreen(new AtlasScreen(SystemMapApi.inSystem() ? AtlasScreen.Layer.GLOBE : AtlasScreen.Layer.GALAXY));
            }
            while (GROUND.consumeClick()) if (mc.player != null && mc.screen == null) {
                mc.setScreen(new AtlasScreen(AtlasScreen.Layer.GROUND));
            }
            while (MINIMAP.consumeClick()) PlanetMinimap.toggle();
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> PlanetMinimap.render(event.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            surveys.clear();
            PlanetMinimap.close();
        });
    }
}
