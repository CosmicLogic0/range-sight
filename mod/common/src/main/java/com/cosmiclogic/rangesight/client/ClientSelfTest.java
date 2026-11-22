package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Automatic test (only when config/rangesight-selftest.txt exists, which CI creates). In a single player world:
 * <ol>
 *   <li>builds a full 4-level beacon pyramid (and, on 1.21+, a trial spawner and a vault) above the player and checks
 *       that Range Sight finds the beacon's 50-block range and the 14 and 4 block trial ranges;</li>
 *   <li>moves the player to the Nether (other dimension edge case), builds another beacon there and checks it is found
 *       and that no spawn chunks are shown outside the Overworld. Right after the switch, the old world's outlines must
 *       be gone (the same thing happens when a world is closed and another one opened).</li>
 * </ol>
 * Logs PASS or FAIL. While it runs, an invisible screen keeps the test runner from closing the game.
 */
final class ClientSelfTest {

    private static final int DONE = 99;
    private static int stage;
    private static int ticks;
    private static Screen hold;
    private static boolean checkedStale;

    private ClientSelfTest() {
    }

    static void tick(Minecraft mc, RangeConfig config) {
        if (!config.selfTest || stage >= DONE || mc.player == null || mc.level == null) {
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        if (hold == null) {
            hold = TestHooks.holdScreen();
        }
        if (mc.screen == null) {
            mc.setScreen(hold); // also after the dimension change, which replaces it with a loading screen
        }
        ticks++;
        if (stage == 0) {
            if (ticks < 40) {
                return; // let the world settle for 2 seconds
            }
            config.setAll(true); // the test may start from a broken (reset) config with every range off
            BlockPos beacon = mc.player.blockPosition().offset(3, 7, 3);
            BlockPos trial = mc.player.blockPosition().offset(-9, 7, -6);
            ResourceKey<Level> dim = mc.level.dimension();
            server.execute(() -> {
                ServerLevel level = server.getLevel(dim);
                if (level == null) {
                    return;
                }
                buildBeacon(level, beacon);
                if (TrialScan.SUPPORTED) {
                    TrialScan.placeForTest(level, trial);
                }
            });
            RangeSight.LOG.info("Range Sight client self-test: built a beacon pyramid at {}{}", beacon,
                    TrialScan.SUPPORTED ? " and a trial spawner + vault at " + trial : "");
            next(1);
        } else if (stage == 1 && ticks >= 30) {
            List<RangeShape> shapes = RangeClient.rescanNow();
            boolean beacon = has(shapes, RangeType.BEACON, 101);
            boolean trial = !TrialScan.SUPPORTED
                    || hasSphere(shapes, RangeMath.TRIAL_SPAWNER_DEFAULT_RANGE) && hasSphere(shapes, RangeMath.VAULT_DEFAULT_RANGE);
            if (!beacon || !trial) {
                fail(mc, (beacon ? "" : "beacon range not found; ") + (trial ? "" : "trial spawner/vault range not found; ")
                        + shapes.size() + " outlines");
                return;
            }
            RangeSight.LOG.info("Range Sight client self-test: Overworld OK ({} outlines{}), going to the Nether",
                    shapes.size(), TrialScan.SUPPORTED ? ", trial spawner 14 + vault 4 found" : "");
            TestHooks.runCommand(server, "execute in minecraft:the_nether run tp @a 0.5 128 0.5");
            next(2);
        } else if (stage == 2) {
            if (mc.level.dimension() != Level.NETHER) {
                if (ticks > 1200) {
                    fail(mc, "could not get to the Nether");
                }
                return;
            }
            if (!checkedStale) {
                checkedStale = true;
                for (RangeShape s : RangeClient.current()) {
                    if (s.type == RangeType.BEACON) {
                        fail(mc, "the Overworld beacon's outline was still drawn in the Nether");
                        return;
                    }
                }
            }
            if (ticks < 60) {
                return; // let the Nether chunks arrive
            }
            BlockPos beacon = mc.player.blockPosition().offset(3, 7, 3);
            server.execute(() -> {
                ServerLevel level = server.getLevel(Level.NETHER);
                if (level != null) {
                    buildBeacon(level, beacon);
                }
            });
            next(3);
        } else if (stage == 3 && ticks >= 30) {
            List<RangeShape> shapes = RangeClient.rescanNow();
            boolean beacon = has(shapes, RangeType.BEACON, 101);
            boolean noSpawnChunks = true;
            for (RangeShape s : shapes) {
                if (s.type == RangeType.SPAWN_CHUNKS) {
                    noSpawnChunks = false;
                }
            }
            if (!beacon || !noSpawnChunks) {
                fail(mc, "Nether: " + (beacon ? "" : "beacon range not found; ")
                        + (noSpawnChunks ? "" : "spawn chunks shown outside the Overworld; ") + shapes.size() + " outlines");
                return;
            }
            RangeSight.LOG.info("Range Sight client self-test PASS: found the beacon's 50-block range in the Overworld"
                    + " and the Nether{} ({} outlines)", TrialScan.SUPPORTED ? ", plus trial spawner and vault ranges" : "",
                    shapes.size());
            finish(mc);
        }
    }

    private static void buildBeacon(ServerLevel level, BlockPos beacon) {
        for (int l = 1; l <= 4; l++) {
            for (int dx = -l; dx <= l; dx++) {
                for (int dz = -l; dz <= l; dz++) {
                    level.setBlock(beacon.offset(dx, -l, dz), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(beacon, Blocks.BEACON.defaultBlockState(), 3);
    }

    private static boolean has(List<RangeShape> shapes, RangeType type, long width) {
        for (RangeShape s : shapes) {
            if (s.type == type && Math.round(s.c - s.a) == width) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSphere(List<RangeShape> shapes, double radius) {
        for (RangeShape s : shapes) {
            if (s.type == RangeType.TRIAL && s.kind == RangeShape.Kind.SPHERE && Math.abs(s.d - radius) < 0.01) {
                return true;
            }
        }
        return false;
    }

    private static void next(int s) {
        stage = s;
        ticks = 0;
    }

    private static void fail(Minecraft mc, String why) {
        RangeSight.LOG.error("Range Sight client self-test FAIL: {}", why);
        finish(mc);
    }

    private static void finish(Minecraft mc) {
        stage = DONE;
        if (mc.screen == hold) {
            mc.setScreen(null);
        }
    }
}
