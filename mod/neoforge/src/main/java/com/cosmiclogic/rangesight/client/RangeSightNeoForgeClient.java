package com.cosmiclogic.rangesight.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge glue: registers keys and forwards tick and render events to the shared client code. */
public final class RangeSightNeoForgeClient {

    private RangeSightNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        // "Config" button in the mod list opens the Range Sight settings screen.
        net.neoforged.fml.ModList.get().getModContainerById(com.cosmiclogic.rangesight.RangeSight.MOD_ID).ifPresent(c ->
                c.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                        (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (container, parent) -> new SettingsScreen(parent)));
        PageShots.init(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, PageShots::onScreenOpening);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> RangeClient.keys().forEach(event::register));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            RangeClient.onClientTick();
            PageShots.tick(); // CI only: does nothing unless config/rangesight-pageshots.txt exists
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                RangeClient.onRender(event.getPoseStack(), event.getCamera().getPosition());
            }
        });
    }
}
