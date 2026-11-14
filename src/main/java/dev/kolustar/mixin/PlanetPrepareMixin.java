package dev.kolustar.mixin;

import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.universe.SystemChunkGenerator;
import dev.kolustar.worldgen.plan.PlanetPlanStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the loading screen up until home is ready, before any player can join. */
@Mixin(MinecraftServer.class)
public abstract class PlanetPrepareMixin {
    @Inject(method = "prepareLevels", at = @At("HEAD"))
    private void kolustar$prepareHome(ChunkProgressListener progress, CallbackInfo ci) {
        var level = ((MinecraftServer) (Object) this).getLevel(PlanetLevels.PLANET);
        if (level != null && level.getChunkSource().getGenerator() instanceof SystemChunkGenerator system)
            PlanetPlanStore.prepareHome(system);
    }
}
