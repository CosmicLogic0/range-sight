package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** The open screen, Minecraft 26.2+ (Minecraft.gui.screen()). 26.1 keeps the older copy (src/java-26.1). */
final class Screens {

    private Screens() {
    }

    static Screen current(Minecraft mc) {
        return mc.gui.screen();
    }

    static void open(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }
}
