package dev.kolustar.mixin;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import dev.kolustar.ship.SableShipStorageAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sable catches write failures inside its void same-address save method. */
@Mixin(value = SubLevelStorage.class, remap = false)
public abstract class SableShipWriteMixin implements SableShipStorageAccess {
    @Unique private boolean kolustar$written;

    @Inject(
            method =
                    "attemptSaveSubLevel(Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;)V",
            at = @At("HEAD"),
            remap = false)
    private void kolustar$begin(
            GlobalSavedSubLevelPointer pointer, SubLevelData data, CallbackInfo ci) {
        kolustar$written = false;
    }

    @Inject(
            method =
                    "attemptSaveSubLevel(Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/ryanhcode/sable/sublevel/storage/region/SubLevelStorageFile;write(ILnet/minecraft/nbt/CompoundTag;)V",
                            shift = At.Shift.AFTER,
                            remap = false),
            remap = false)
    private void kolustar$written(
            GlobalSavedSubLevelPointer pointer, SubLevelData data, CallbackInfo ci) {
        kolustar$written = true;
    }

    @Override
    public boolean kolustar$writeSucceeded() {
        return kolustar$written;
    }
}
