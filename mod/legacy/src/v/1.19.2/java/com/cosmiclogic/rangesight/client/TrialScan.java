package com.cosmiclogic.rangesight.client;

import java.util.List;

/** Minecraft before 1.21 has no trial spawners or vaults: nothing to add. */
final class TrialScan {

    static final boolean SUPPORTED = false;

    private TrialScan() {
    }

    static boolean add(Object blockEntity, RangeConfig config, List<RangeShape> out) {
        return false;
    }
}
