package com.cosmiclogic.rangesight.client;

import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 26.2+: SubmitCustomGeometryEvent fires right before Minecraft finishes collecting the frame's gizmos. */
final class RangeRenderHook {

    private RangeRenderHook() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, event -> RangeClient.onGizmos());
    }
}
