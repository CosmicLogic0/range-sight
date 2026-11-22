package com.cosmiclogic.rangesight;

import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

/** Forge 1.16.5 reads no displayTest from mods.toml; it has to be registered in code. */
final class ForgeCompat {

    private ForgeCompat() {
    }

    /** Lets players join servers without Range Sight (and servers show no red ✗), because it is client-side only. */
    static void clientSideOnly() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
    }
}
