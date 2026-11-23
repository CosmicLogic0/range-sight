package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 1.21.11 glue (1.21.9+: one render event class per stage). */
public final class RangeSightNeoForgeClient {

    private RangeSightNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        // "Config" button in the mod list opens the Range Sight settings screen.
        net.neoforged.fml.ModList.get().getModContainerById(com.cosmiclogic.rangesight.RangeSight.MOD_ID).ifPresent(c ->
                c.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                        (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (container, parent) -> new SettingsScreen(parent)));
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> RangeClient.keys().forEach(e::register));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> RangeClient.onClientTick());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.AfterTranslucentBlocks.class, e ->
                RangeClient.onRender(e.getPoseStack(), Minecraft.getInstance().gameRenderer.getMainCamera().position()));
    }
}
