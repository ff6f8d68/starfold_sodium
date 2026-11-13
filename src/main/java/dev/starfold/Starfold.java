package dev.starfold;

import dev.starfold.network.StarfoldNetwork;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("starfold")
public final class Starfold {
    public Starfold(IEventBus modBus) {
        StarfoldNetwork.initialize(modBus);
        StarfoldServer.initialize(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StarfoldClient.initialize(modBus);
        }
    }
}
