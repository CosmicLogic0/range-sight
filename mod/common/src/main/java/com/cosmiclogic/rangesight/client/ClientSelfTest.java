package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Automatic test (only when config/rangesight.json has "selfTest": true, which CI sets):
 * in a single player world, builds a full 4-level iron beacon pyramid above the player,
 * then checks that Range Sight finds the beacon's 50-block range. Logs PASS or FAIL.
 */
final class ClientSelfTest {

    private static int stage;
    private static int ticks;

    private ClientSelfTest() {
    }

    static void tick(Minecraft mc, RangeConfig config) {
        if (!config.selfTest || stage >= 2 || mc.player == null || mc.level == null) {
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        if (stage == 0) {
            if (++ticks < 40) {
                return; // let the world settle for 2 seconds
            }
            BlockPos beacon = mc.player.blockPosition().offset(3, 7, 3);
            ResourceKey<Level> dim = mc.level.dimension();
            server.execute(() -> {
                ServerLevel level = server.getLevel(dim);
                if (level == null) {
                    return;
                }
                for (int l = 1; l <= 4; l++) {
                    for (int dx = -l; dx <= l; dx++) {
                        for (int dz = -l; dz <= l; dz++) {
                            level.setBlock(beacon.offset(dx, -l, dz), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                        }
                    }
                }
                level.setBlock(beacon, Blocks.BEACON.defaultBlockState(), 3);
            });
            RangeSight.LOG.info("Range Sight client self-test: built a beacon pyramid at {}", beacon);
            stage = 1;
            ticks = 0;
        } else if (++ticks >= 30) {
            List<RangeShape> shapes = RangeClient.rescanNow();
            boolean found = false;
            for (RangeShape s : shapes) {
                if (s.type == RangeType.BEACON && Math.round(s.c - s.a) == 101) {
                    found = true;
                }
            }
            if (found) {
                RangeSight.LOG.info("Range Sight client self-test PASS: found the beacon's 50-block range ({} outlines)", shapes.size());
            } else {
                RangeSight.LOG.error("Range Sight client self-test FAIL: beacon range not found ({} outlines)", shapes.size());
            }
            stage = 2;
        }
    }
}
