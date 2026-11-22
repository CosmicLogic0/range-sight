package com.cosmiclogic.rangesight.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Small version-specific helpers for the automatic client test. Minecraft 1.16.5 and 1.18.2 get their own copy
 * (different text and command methods there).
 */
final class TestHooks {

    private TestHooks() {
    }

    /** An invisible screen that keeps the test runner from closing the game while the self-test is still going. */
    static Screen holdScreen() {
        return new Hold();
    }

    static void runCommand(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    private static final class Hold extends Screen {
        Hold() {
            super(Component.empty());
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
