package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 1.16.5 glue: keys via ClientRegistry, drawing via RenderWorldLastEvent. */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init() {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent e) ->
                e.enqueueWork(() -> RangeClient.keys().forEach(ClientRegistry::registerKeyBinding)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                RangeClient.onClientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderWorldLastEvent e) ->
                RangeClient.onRender(e.getMatrixStack(), Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()));
    }
}
