package com.cosmiclogic.rangesight;

import com.cosmiclogic.rangesight.client.RangeClient12;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Range Sight for Forge 1.12.2. Client-side: on a server it only loads, and any server works with it. */
@Mod(modid = RangeSight.MOD_ID, name = RangeSight.MOD_NAME, version = "0.1.0",
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*")
public class RangeSightForge {

    private static boolean client;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        RangeSight.init("Forge");
        client = event.getSide().isClient();
        if (client) {
            ClientStart.preInit(event.getModConfigurationDirectory().toPath());
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (client) {
            ClientStart.init();
        }
    }

    /** In its own class so servers never load client code. */
    static final class ClientStart {
        static void preInit(java.nio.file.Path configDir) {
            RangeClient12.preInit(configDir);
        }

        static void init() {
            RangeClient12.init();
        }
    }
}
