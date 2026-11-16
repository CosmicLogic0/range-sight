package com.cosmiclogic.rangesight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 1.21.11 glue: one bus per event; drawing is hooked by mixin.LevelRendererMixin (no world render event). */
public final class RangeSightForgeClient {

    private RangeSightForgeClient() {
    }

    public static void init() {
        RangeClient.init(FMLPaths.CONFIGDIR.get());
        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(e -> RangeClient.keys().forEach(e::register));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> RangeClient.onClientTick());
    }

    /** Called at the end of LevelRenderer.renderLevel: the camera rotation is applied by hand there. */
    public static void afterLevel() {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(cam.xRot()));
        pose.mulPose(Axis.YP.rotationDegrees(cam.yRot() + 180f));
        RangeClient.onRender(pose, cam.position());
    }
}
