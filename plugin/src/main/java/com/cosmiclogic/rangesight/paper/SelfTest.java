package com.cosmiclogic.rangesight.paper;

import com.cosmiclogic.logiclib.paper.LogicVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Console-only check used by the automatic tests: "range selftest".
 * Builds a beacon pyramid, a conduit frame, a spawner and sculk blocks high in the sky,
 * scans them like /range does, compares with the expected vanilla ranges, then removes them.
 */
final class SelfTest {

    private SelfTest() {
    }

    static boolean run(RangeDrawer drawer, World world, Logger log) {
        Location spawn = world.getSpawnLocation();
        int x = spawn.getBlockX() + 2000;
        int z = spawn.getBlockZ() + 2000;
        int y = 150;
        world.getChunkAt(x >> 4, z >> 4).load(true);

        List<Block> placed = new ArrayList<>();
        List<String> expected = new ArrayList<>();

        // Beacon on a full 4-level iron pyramid -> 50 blocks.
        Material iron = material("IRON_BLOCK");
        for (int level = 1; level <= 4; level++) {
            for (int dx = -level; dx <= level; dx++) {
                for (int dz = -level; dz <= level; dz++) {
                    set(world, x + dx, y - level, z + dz, iron, placed);
                }
            }
        }
        set(world, x, y, z, material("BEACON"), placed);
        expected.add("beacon square 50");

        // Spawner -> 16 (activation) + 4 (spawn area).
        Material spawner = material("SPAWNER", "MOB_SPAWNER");
        set(world, x + 20, y, z, spawner, placed);
        expected.add("spawner sphere 16");
        expected.add("spawner box 4");

        // Conduit with all 42 frame blocks -> 96 (1.13+).
        Material conduit = material("CONDUIT");
        if (conduit != null) {
            Material prismarine = material("PRISMARINE");
            int cx = x - 20;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (RangeMath.isConduitFramePosition(dx, dy, dz)) {
                            set(world, cx + dx, y + dy, z + dz, prismarine, placed);
                        }
                    }
                }
            }
            set(world, cx, y, z, conduit, placed);
            expected.add("conduit sphere 96");
        }

        // Sculk (1.19+ for shriekers, 1.20+ for calibrated sensors).
        Material sensor = material("SCULK_SENSOR");
        if (sensor != null) {
            set(world, x, y, z + 20, sensor, placed);
            expected.add("sculk sphere 8");
        }
        Material calibrated = material("CALIBRATED_SCULK_SENSOR");
        if (calibrated != null) {
            set(world, x, y, z - 20, calibrated, placed);
            expected.add("sculk sphere 16");
        }
        Material shrieker = material("SCULK_SHRIEKER");
        if (shrieker != null) {
            set(world, x + 10, y, z + 10, shrieker, placed);
            expected.add("sculk sphere 8");
            expected.add("sculk sphere 40");
        }

        List<String> found = new ArrayList<>();
        for (RangeDrawer.Source source : drawer.findSources(world, new Location(world, x, y, z), false)) {
            found.add(source.toString());
        }

        int[] chunks = RangeDrawer.spawnChunks(world);
        log.info("Self-test on Minecraft " + LogicVersion.current() + ": expected " + expected + ", found " + found
                + ", spawn chunks " + (chunks == null ? "none" : Arrays.toString(chunks)));

        // Clean up (top to bottom so nothing pops off).
        for (int i = placed.size() - 1; i >= 0; i--) {
            placed.get(i).setType(Material.AIR);
        }

        List<String> missing = new ArrayList<>(expected);
        for (String f : found) {
            missing.remove(f);
        }
        List<String> extra = new ArrayList<>(found);
        for (String e : expected) {
            extra.remove(e);
        }
        boolean spawnOk = LogicVersion.current().isAtLeast(1, 21, 9) ? chunks == null : chunks != null;
        if (missing.isEmpty() && extra.isEmpty() && spawnOk) {
            log.info("Range Sight self-test PASS");
            return true;
        }
        log.severe("Range Sight self-test FAIL: missing " + missing + ", unexpected " + extra + ", spawn chunks ok " + spawnOk);
        return false;
    }

    private static void set(World world, int x, int y, int z, Material type, List<Block> placed) {
        if (type == null) {
            return;
        }
        Block block = world.getBlockAt(x, y, z);
        block.setType(type);
        placed.add(block);
    }

    private static Material material(String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Not on this version.
            }
        }
        return null;
    }
}
