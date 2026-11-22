package com.cosmiclogic.rangesight;

/** Forge 1.19+: the server-list check is set in mods.toml (displayTest = "IGNORE_ALL_VERSION"), nothing to do here. */
final class ForgeCompat {

    private ForgeCompat() {
    }

    /** Lets players join servers without Range Sight (and servers show no red ✗), because it is client-side only. */
    static void clientSideOnly() {
    }
}
