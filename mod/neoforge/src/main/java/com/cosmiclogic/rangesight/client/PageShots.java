package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CI only, never active for players: when config/rangesight-pageshots.txt exists, builds a small scene
 * (beacon pyramid, spawner, sculk sensor) in the single player world the client test opens, moves the camera,
 * hides the HUD and saves real screenshots for the CurseForge page into screenshots/.
 */
final class PageShots {

    private static Boolean active;
    private static boolean holding;
    private static int ticks;
    private static BlockPos beacon;
    private static BlockPos spawner;

    private PageShots() {
    }

    static void tick(Path configDir) {
        if (active == null) {
            active = Files.exists(configDir.resolve("rangesight-pageshots.txt"));
        }
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.player == null || mc.level == null) {
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        if (!holding) {
            // Wait until the world is really shown, then keep an empty see-through screen open: the client test
            // quits a few ticks after the last screen closes, and this gives the scene time to load and be shot.
            if (mc.screen != null) {
                return;
            }
            mc.setScreen(new HoldScreen());
            holding = true;
            RangeSight.LOG.info("Range Sight page shots: world shown, holding the client open");
        }
        ticks++;
        try {
            if (ticks == 2) {
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
                mc.options.hideGui = true;
                BlockPos p = mc.player.blockPosition();
                server.execute(() -> build(server, p));
            } else if (ticks == 12 && beacon != null) {
                view(server, beacon, -20, 12, -26);
            } else if (ticks == 45) {
                RangeClient.rescanNow();
            } else if (ticks == 55) {
                shot(mc, "rangesight-page-1.png");
            } else if (ticks == 60 && beacon != null) {
                view(server, spawner, 9, 6, 11);
            } else if (ticks == 95) {
                RangeClient.rescanNow();
            } else if (ticks == 105) {
                shot(mc, "rangesight-page-2.png");
            } else if (ticks == 110 && beacon != null) {
                view(server, beacon, -40, 38, 46);
            } else if (ticks == 145) {
                RangeClient.rescanNow();
            } else if (ticks == 155) {
                shot(mc, "rangesight-page-3.png");
            } else if (ticks >= 165) {
                done(mc);
            }
        } catch (Throwable t) {
            RangeSight.LOG.error("Range Sight page shots failed", t);
            done(mc);
        }
    }

    private static void done(Minecraft mc) {
        active = false;
        mc.options.hideGui = false;
        if (mc.screen instanceof HoldScreen) {
            mc.setScreen(null);
        }
    }

    /** Draws nothing, so the world shows through; doesn't pause the game. */
    private static final class HoldScreen extends Screen {
        HoldScreen() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

    private static void build(IntegratedServer server, BlockPos player) {
        ServerLevel level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.setDayTime(5000L);
        level.setWeatherParameters(20000, 0, false, false);
        int bx = player.getX() + 12, bz = player.getZ() + 12;
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        // clear a flat area (and the air above it) so trees don't hide the scene; fill below so it isn't floating
        for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -24; dz <= 24; dz++) {
                for (int dy = 0; dy <= 40; dy++) {
                    BlockPos q = new BlockPos(bx + dx, h + dy, bz + dz);
                    if (!level.getBlockState(q).isAir()) {
                        level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                level.setBlock(new BlockPos(bx + dx, h - 1, bz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int dy = 2; dy <= 12; dy++) {
                    BlockPos q = new BlockPos(bx + dx, h - dy, bz + dz);
                    if (level.getBlockState(q).isAir() || !level.getFluidState(q).isEmpty()) {
                        level.setBlock(q, Blocks.DIRT.defaultBlockState(), 2);
                    }
                }
            }
        }
        BlockPos b = new BlockPos(bx, h + 4, bz);
        for (int l = 1; l <= 4; l++) {
            for (int dx = -l; dx <= l; dx++) {
                for (int dz = -l; dz <= l; dz++) {
                    level.setBlock(b.offset(dx, -l, dz), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(b, Blocks.BEACON.defaultBlockState(), 3);
        spawner = new BlockPos(bx - 10, h, bz + 7);
        level.setBlock(spawner, Blocks.SPAWNER.defaultBlockState(), 3);
        level.setBlock(new BlockPos(bx + 8, h, bz - 9), Blocks.SCULK_SENSOR.defaultBlockState(), 3);
        level.setBlock(new BlockPos(bx + 9, h, bz + 9), Blocks.SCULK_SHRIEKER.defaultBlockState(), 3);
        beacon = b;
        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
        sp.setGameMode(GameType.CREATIVE);
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        RangeSight.LOG.info("Range Sight page shots: scene built at {}", b);
    }

    /** Moves the player to target + (dx, dy, dz), looking at the target. */
    private static void view(IntegratedServer server, BlockPos b, int dx, int dy, int dz) {
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            double x = b.getX() + 0.5 + dx, y = b.getY() + dy, z = b.getZ() + 0.5 + dz;
            double tx = b.getX() + 0.5 - x, ty = b.getY() - 1 - (y + sp.getEyeHeight()), tz = b.getZ() + 0.5 - z;
            float yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
            float pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.sqrt(tx * tx + tz * tz)));
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(server.overworld(), x, y, z, yaw, pitch);
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                msg -> RangeSight.LOG.info("Range Sight page shot saved: {}", msg.getString()));
    }
}
