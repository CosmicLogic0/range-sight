package com.cosmiclogic.rangesight.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;

/** Test helpers for Minecraft before 1.19 (TextComponent, performCommand). */
final class TestHooks {

    private TestHooks() {
    }

    static Screen holdScreen() {
        return new Hold();
    }

    static void runCommand(MinecraftServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack(), command);
    }

    private static final class Hold extends Screen {
        Hold() {
            super(new TextComponent(""));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }
    }
}
