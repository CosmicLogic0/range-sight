package com.cosmiclogic.rangesight.client;

import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 26.1.x: ExtractLevelRenderStateEvent fires right after Minecraft emits its own gizmos for the frame. */
final class RangeRenderHook {

    private RangeRenderHook() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ExtractLevelRenderStateEvent.class, event -> RangeClient.onGizmos());
    }
}
