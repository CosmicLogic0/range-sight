package com.cosmiclogic.rangesight;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

/** Forge 1.18.2 reads no displayTest from mods.toml; it has to be registered in code. */
final class ForgeCompat {

    private ForgeCompat() {
    }

    /** Lets players join servers without Range Sight (and servers show no red ✗), because it is client-side only. */
    static void clientSideOnly() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
    }
}
