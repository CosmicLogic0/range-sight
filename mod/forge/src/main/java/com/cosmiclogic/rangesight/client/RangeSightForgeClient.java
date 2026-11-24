package com.cosmiclogic.rangesight.client;

import com.mojang.blaze3d.vertex.PoseStack;
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

    public static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        // "Config" button in the mod list opens the Range Sight settings screen.
        context.registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
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
            // Forge 52 hands over the camera rotation as a matrix, not a PoseStack.
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(event.getPoseStack());
            RangeClient.onRender(poseStack, event.getCamera().getPosition());
        }
    }
}
