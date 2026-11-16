package com.cosmiclogic.rangesight.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 1.19.2 / 1.20.1 glue. */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init() {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterKeyMappingsEvent e) -> RangeClient.keys().forEach(e::register));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                RangeClient.onClientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                RangeClient.onRender(e.getPoseStack(), e.getCamera().getPosition());
            }
        });
    }
}
