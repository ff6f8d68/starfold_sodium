package dev.kolustar;

import dev.kolustar.network.KolustarNetwork;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("kolustar")
public final class Kolustar {
    public Kolustar(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, KolustarConfig.COMMON_SPEC);
        KolustarNetwork.initialize(modBus);
        KolustarServer.initialize(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, KolustarConfig.CLIENT_SPEC);
            dev.kolustar.client.ClientConfigScreen.register(modContainer);
            KolustarClient.initialize(modBus);
        }
    }
}
