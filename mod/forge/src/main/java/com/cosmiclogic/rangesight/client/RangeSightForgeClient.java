package com.cosmiclogic.rangesight.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge glue: registers keys and forwards tick and render events to the shared client code. */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init(IEventBus modBus) {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(RangeSightForgeClient::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(RangeSightForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RangeSightForgeClient::onRenderLevel);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        RangeClient.keys().forEach(event::register);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RangeClient.onClientTick();
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            RangeClient.onRender(event.getPoseStack(), event.getCamera().getPosition());
        }
    }
}
