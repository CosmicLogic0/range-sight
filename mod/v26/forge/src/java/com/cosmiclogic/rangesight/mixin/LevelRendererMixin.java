package com.cosmiclogic.rangesight.mixin;

import com.cosmiclogic.rangesight.client.RangeClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 26.2+: add Range Sight's outlines right before Minecraft finishes collecting the frame's gizmos. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "submitFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
    private void rangesight$beforeGizmos(CallbackInfo ci) {
        try {
            RangeClient.onGizmos();
        } catch (Throwable ignored) {
            // never break the game's frame
        }
    }
}
