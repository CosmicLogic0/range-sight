package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
    private static int ticks;
    private static BlockPos beacon;

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
        ticks++;
        try {
            if (ticks == 2) {
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
                BlockPos p = mc.player.blockPosition();
                server.execute(() -> build(server, p));
            } else if (ticks == 6 && beacon != null) {
                view(server, -20, 12, -26);
            } else if (ticks == 24) {
                RangeClient.rescanNow();
                mc.options.hideGui = true;
            } else if (ticks == 30) {
                shot(mc, "rangesight-page-1.png");
            } else if (ticks == 32 && beacon != null) {
                view(server, 30, 30, 10);
            } else if (ticks == 50) {
                RangeClient.rescanNow();
            } else if (ticks == 56) {
                shot(mc, "rangesight-page-2.png");
            } else if (ticks == 58 && beacon != null) {
                view(server, -4, 6, 20);
            } else if (ticks == 76) {
                RangeClient.rescanNow();
            } else if (ticks == 82) {
                shot(mc, "rangesight-page-3.png");
                active = false;
            }
        } catch (Throwable t) {
            RangeSight.LOG.error("Range Sight page shots failed", t);
            active = false;
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
        // clear a flat area so trees don't hide the scene
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                for (int dy = 0; dy <= 24; dy++) {
                    level.setBlock(new BlockPos(bx + dx, h + dy, bz + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(new BlockPos(bx + dx, h - 1, bz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
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
        level.setBlock(new BlockPos(bx - 10, h, bz + 7), Blocks.SPAWNER.defaultBlockState(), 3);
        level.setBlock(new BlockPos(bx + 8, h, bz - 9), Blocks.SCULK_SENSOR.defaultBlockState(), 3);
        level.setBlock(new BlockPos(bx + 9, h, bz + 9), Blocks.SCULK_SHRIEKER.defaultBlockState(), 3);
        beacon = b;
        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
        sp.setGameMode(GameType.CREATIVE);
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        RangeSight.LOG.info("Range Sight page shots: scene built at {}", b);
    }

    /** Moves the player to beacon + (dx, dy, dz), looking at the beacon. */
    private static void view(IntegratedServer server, int dx, int dy, int dz) {
        BlockPos b = beacon;
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
