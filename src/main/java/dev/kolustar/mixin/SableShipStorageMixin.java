package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.ship.SableShipHoldingAccess;
import dev.kolustar.ship.SableShipStorageAccess;
import dev.kolustar.ship.ShipDormancy;
import dev.kolustar.ship.ShipRecords;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Follow both live and holding writes; publish SavedData only after Sable's flush. */
@Mixin(value = SubLevelHoldingChunkMap.class, remap = false)
public abstract class SableShipStorageMixin implements SableShipHoldingAccess {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private SubLevelStorage storage;
    @Unique private boolean kolustar$flushed;

    @Override
    public SubLevelData kolustar$readShip(GlobalSavedSubLevelPointer pointer) {
        return storage.attemptLoadSubLevel(pointer.chunkPos(), pointer.local());
    }

    @Inject(
            method =
                    "moveAndSaveSubLevel(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Lnet/minecraft/world/level/ChunkPos;)Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;",
            at = @At("RETURN"),
            remap = false)
    private void kolustar$pointer(
            ServerSubLevel hull,
            SubLevelData data,
            GlobalSavedSubLevelPointer old,
            ChunkPos chunk,
            CallbackInfoReturnable<GlobalSavedSubLevelPointer> ci) {
        var pointer = ci.getReturnValue();
        if (pointer != null
                && (!pointer.equals(old)
                        || ((SableShipStorageAccess) storage).kolustar$writeSucceeded()))
            ShipRecords.pointerSaved(level, data, pointer);
    }

    @Inject(
            method =
                    "snatchAndLoad(Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Ljava/util/UUID;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void kolustar$deferWake(
            GlobalSavedSubLevelPointer pointer, java.util.UUID ship, CallbackInfo ci) {
        if (ShipDormancy.defer(level, ship)) ci.cancel();
    }

    @WrapOperation(
            method = "loadHoldingSubLevel(Ldev/ryanhcode/sable/sublevel/storage/HoldingSubLevel;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelSerializer;fullyLoad(Lnet/minecraft/server/level/ServerLevel;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;)Ldev/ryanhcode/sable/sublevel/ServerSubLevel;",
                            remap = false),
            remap = false)
    private ServerSubLevel kolustar$loadAtPlace(
            ServerLevel level, SubLevelData originalData, Operation<ServerSubLevel> original) {
        var data = ShipDormancy.loadInput(level, originalData);
        var hull = original.call(level, data);
        ShipDormancy.loaded(level, data, hull);
        return hull;
    }

    @Inject(method = "saveAll()V", at = @At("HEAD"), remap = false)
    private void kolustar$beginSave(CallbackInfo ci) {
        kolustar$flushed = false;
    }

    @WrapOperation(
            method = "saveAll()V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelStorage;flush()V",
                            remap = false),
            remap = false)
    private void kolustar$flush(SubLevelStorage storage, Operation<Void> original) {
        original.call(storage);
        kolustar$flushed = true;
    }

    @Inject(method = "saveAll()V", at = @At("RETURN"), remap = false)
    private void kolustar$saveRecords(CallbackInfo ci) {
        if (kolustar$flushed && PlanetLevels.is(level)) level.getDataStorage().save();
    }
}
