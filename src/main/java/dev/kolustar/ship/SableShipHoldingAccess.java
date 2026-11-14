package dev.kolustar.ship;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;

/** Probe reads may inspect a saved payload without snatching or loading its hull. */
public interface SableShipHoldingAccess {
    SubLevelData kolustar$readShip(GlobalSavedSubLevelPointer pointer);
}
