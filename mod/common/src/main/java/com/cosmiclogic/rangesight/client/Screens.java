package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** The open screen. Minecraft 26.2 moved it from Minecraft to Minecraft.gui, so 26.2+ gets its own copy. */
final class Screens {

    private Screens() {
    }

    static Screen current(Minecraft mc) {
        return mc.screen;
    }

    static void open(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }
}
