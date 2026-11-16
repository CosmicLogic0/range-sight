package com.cosmiclogic.rangesight.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 26.x glue: keys, ticks, and the gizmo hook ({@link RangeRenderHook}, which differs on 26.1). */
public final class RangeSightNeoForgeClient {

    private RangeSightNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> RangeClient.keys().forEach(event::register));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> RangeClient.onClientTick());
        RangeRenderHook.register();
    }
}
