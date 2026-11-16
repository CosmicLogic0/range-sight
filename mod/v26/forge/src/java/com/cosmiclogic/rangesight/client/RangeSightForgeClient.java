package com.cosmiclogic.rangesight.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 26.x glue: keys and ticks. Drawing is hooked by {@code mixin.LevelRendererMixin} (Forge 26 has no world render event). */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init() {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        RegisterKeyMappingsEvent.BUS.addListener(event -> RangeClient.keys().forEach(event::register));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> RangeClient.onClientTick());
    }
}
