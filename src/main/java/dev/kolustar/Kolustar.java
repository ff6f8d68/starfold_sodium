package dev.kolustar;

import dev.kolustar.network.KolustarNetwork;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("kolustar")
public final class Kolustar {
    public Kolustar(IEventBus modBus) {
        KolustarNetwork.initialize(modBus);
        KolustarServer.initialize(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KolustarClient.initialize(modBus);
        }
    }
}
