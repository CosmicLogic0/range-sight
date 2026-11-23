package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 1.18.2 glue: keys via ClientRegistry, drawing via RenderLevelLastEvent. */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init() {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        // "Config" button in the mod list opens the Range Sight settings screen.
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new SettingsScreen(parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent e) ->
                e.enqueueWork(() -> RangeClient.keys().forEach(ClientRegistry::registerKeyBinding)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                RangeClient.onClientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelLastEvent e) ->
                RangeClient.onRender(e.getPoseStack(), Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()));
    }
}
