package com.cosmiclogic.rangesight.mixin;

import com.cosmiclogic.rangesight.client.RangeSightForgeClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 1.21.11 has no world render event: draw the outlines after the world has been rendered. */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void rangesight$afterLevel(CallbackInfo ci) {
        try {
            RangeSightForgeClient.afterLevel();
        } catch (Throwable ignored) {
            // never break the game's frame
        }
    }
}
