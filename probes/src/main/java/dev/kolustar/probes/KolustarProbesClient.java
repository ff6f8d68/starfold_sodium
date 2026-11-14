package dev.kolustar.probes;

import dev.kolustar.DevelopmentHooks;
import dev.kolustar.dev.DevFeelProbe;
import dev.kolustar.dev.DevPlanetSoundProbe;
import dev.kolustar.dev.DevProbe;
import dev.kolustar.planet.PlanetSoundClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = "kolustar_probes", dist = Dist.CLIENT)
public final class KolustarProbesClient {
    public KolustarProbesClient() {
        DevelopmentHooks.beforeKeys = () -> DevFeelProbe.prepare(Minecraft.getInstance());
        PlanetSoundClient.observer = new PlanetSoundClient.Observer() {
            @Override public boolean enabled() { return DevPlanetSoundProbe.enabled(); }
            @Override public void created(SoundInstance sound) { DevPlanetSoundProbe.created(sound); }
            @Override public void scaled(SoundInstance sound, double density, boolean self, float scale) { DevPlanetSoundProbe.scaled(sound, density, self, scale); }
            @Override public void calculated(SoundInstance sound, float raw, float result, String phase) { DevPlanetSoundProbe.calculated(sound, raw, result, phase); }
            @Override public void refreshing(boolean value) { DevPlanetSoundProbe.refreshing(value); }
        };
        DevProbe.initialize();
    }
}
