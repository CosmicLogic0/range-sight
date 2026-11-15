package com.cosmiclogic.rangesight.paper;

import com.cosmiclogic.logiclib.paper.LogicParticles;
import com.cosmiclogic.logiclib.paper.LogicVersion;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Finds range sources near a player and draws their outlines with LogicLib particles.
 * Spheres are drawn as one circle at the player's height (the slice you are standing in),
 * squares as one ring at the player's height, so the edge is always where you are.
 */
final class RangeDrawer {

    /** Look for blocks this many chunks around the player. */
    private static final int SCAN_CHUNKS = 4;
    /** Rescan blocks every this many milliseconds (outlines are still redrawn every second). */
    private static final long RESCAN_MS = 3000;
    /** Never draw more than this many sources per player, to protect the server and the player. */
    private static final int MAX_SOURCES = 12;
    private static final double LINE_STEP = 0.5;

    private static final Set<String> BEACON_BASE = new HashSet<>(Arrays.asList(
            "IRON_BLOCK", "GOLD_BLOCK", "EMERALD_BLOCK", "DIAMOND_BLOCK", "NETHERITE_BLOCK"));
    private static final Set<String> CONDUIT_FRAME = new HashSet<>(Arrays.asList(
            "PRISMARINE", "PRISMARINE_BRICKS", "SEA_LANTERN", "DARK_PRISMARINE"));

    private final RangeSightPlugin plugin;
    private final Map<UUID, Scan> scans = new HashMap<>();

    RangeDrawer(RangeSightPlugin plugin) {
        this.plugin = plugin;
    }

    void draw(Player player, Set<RangeType> types) {
        Location eye = player.getLocation();
        double y = eye.getY() + 0.15;
        World world = player.getWorld();

        Scan scan = scans.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (scan == null || now - scan.time > RESCAN_MS || !scan.world.equals(world.getName())) {
            scan = new Scan(world.getName(), now, findSources(world, player.getLocation(), types.contains(RangeType.RAID)));
            scans.put(player.getUniqueId(), scan);
        }

        for (Source s : scan.sources) {
            if (!types.contains(s.type)) {
                continue;
            }
            switch (s.kind) {
                case SQUARE:
                    if (y >= s.minY) {
                        square(player, world, s.x1, s.z1, s.x2, s.z2, y, s.type.color);
                    }
                    break;
                case SPHERE:
                    double r = RangeMath.sliceRadius(s.radius, y - s.cy);
                    if (r > 0.5) {
                        LogicParticles.circle(player, new Location(world, s.cx, y, s.cz), r, s.color != null ? s.color : s.type.color);
                    }
                    break;
                case BOX:
                    LogicParticles.box(player, s.x1, s.minY, s.z1, s.x2, s.maxY, s.z2, s.type.color);
                    break;
                default:
                    break;
            }
        }

        if (types.contains(RangeType.SPAWN_CHUNKS)) {
            int[] chunks = spawnChunks(world);
            if (chunks != null) {
                square(player, world, chunks[0] * 16, chunks[1] * 16, (chunks[2] + 1) * 16, (chunks[3] + 1) * 16, y, RangeType.SPAWN_CHUNKS.color);
            }
        }
    }

    void forget(UUID player) {
        scans.remove(player);
    }

    private static void square(Player player, World world, double x1, double z1, double x2, double z2, double y, Color color) {
        Location a = new Location(world, x1, y, z1);
        Location b = new Location(world, x2, y, z1);
        Location c = new Location(world, x2, y, z2);
        Location d = new Location(world, x1, y, z2);
        LogicParticles.line(player, a, b, LINE_STEP, color);
        LogicParticles.line(player, b, c, LINE_STEP, color);
        LogicParticles.line(player, c, d, LINE_STEP, color);
        LogicParticles.line(player, d, a, LINE_STEP, color);
    }

    /** All range sources near a location, nearest first (at most {@link #MAX_SOURCES}). */
    List<Source> findSources(World world, Location at, boolean raids) {
        int pcx = at.getBlockX() >> 4;
        int pcz = at.getBlockZ() >> 4;
        List<Source> found = new ArrayList<>();

        for (int cx = pcx - SCAN_CHUNKS; cx <= pcx + SCAN_CHUNKS; cx++) {
            for (int cz = pcz - SCAN_CHUNKS; cz <= pcz + SCAN_CHUNKS; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                for (BlockState state : world.getChunkAt(cx, cz).getTileEntities()) {
                    addSource(world, state, found);
                }
            }
        }
        if (raids) {
            addRaids(world, found);
        }

        found.sort((a, b) -> Double.compare(a.distanceSq(at), b.distanceSq(at)));
        return found.size() > MAX_SOURCES ? new ArrayList<>(found.subList(0, MAX_SOURCES)) : found;
    }

    private void addSource(World world, BlockState state, List<Source> out) {
        String name = state.getType().name();
        int x = state.getX();
        int y = state.getY();
        int z = state.getZ();
        switch (name) {
            case "BEACON": {
                int range = RangeMath.beaconRange(beaconLevels(world, x, y, z));
                if (range > 0) {
                    // Vanilla: x/z from beacon - range to beacon + 1 + range, from beacon y - range up to the sky.
                    out.add(Source.square(RangeType.BEACON, x - range, z - range, x + 1 + range, z + 1 + range, y - range));
                }
                break;
            }
            case "CONDUIT": {
                int range = RangeMath.conduitRange(conduitFrames(world, x, y, z));
                if (range > 0) {
                    out.add(Source.sphere(RangeType.CONDUIT, x + 0.5, y + 0.5, z + 0.5, range, null));
                }
                break;
            }
            case "SPAWNER":
            case "MOB_SPAWNER": {
                int playerRange = RangeMath.SPAWNER_DEFAULT_PLAYER_RANGE;
                int spawnRange = RangeMath.SPAWNER_DEFAULT_SPAWN_RANGE;
                if (state instanceof CreatureSpawner) {
                    try {
                        playerRange = ((CreatureSpawner) state).getRequiredPlayerRange();
                        spawnRange = ((CreatureSpawner) state).getSpawnRange();
                    } catch (Throwable ignored) {
                        // Older API: keep the defaults.
                    }
                }
                out.add(Source.sphere(RangeType.SPAWNER, x + 0.5, y + 0.5, z + 0.5, playerRange, null));
                out.add(Source.box(RangeType.SPAWNER, x + 0.5 - spawnRange, y - 1, z + 0.5 - spawnRange,
                        x + 0.5 + spawnRange, y + 2, z + 0.5 + spawnRange));
                break;
            }
            case "SCULK_SENSOR":
            case "CALIBRATED_SCULK_SENSOR":
                out.add(Source.sphere(RangeType.SCULK, x + 0.5, y + 0.5, z + 0.5,
                        RangeMath.sculkSensorRange(name.startsWith("CALIBRATED")), null));
                break;
            case "SCULK_SHRIEKER":
                out.add(Source.sphere(RangeType.SCULK, x + 0.5, y + 0.5, z + 0.5, RangeMath.SHRIEKER_LISTEN_RANGE, null));
                out.add(Source.sphere(RangeType.SCULK, x + 0.5, y + 0.5, z + 0.5, RangeMath.SHRIEKER_DARKNESS_RANGE,
                        Color.fromRGB(0x7A3CC8)));
                break;
            default:
                break;
        }
    }

    private static int beaconLevels(World world, int x, int y, int z) {
        int levels = 0;
        for (int level = 1; level <= 4; level++) {
            int by = y - level;
            if (by < -64) {
                break;
            }
            for (int bx = x - level; bx <= x + level; bx++) {
                for (int bz = z - level; bz <= z + level; bz++) {
                    if (!BEACON_BASE.contains(world.getBlockAt(bx, by, bz).getType().name())) {
                        return levels;
                    }
                }
            }
            levels = level;
        }
        return levels;
    }

    private static int conduitFrames(World world, int x, int y, int z) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (RangeMath.isConduitFramePosition(dx, dy, dz)
                            && CONDUIT_FRAME.contains(world.getBlockAt(x + dx, y + dy, z + dz).getType().name())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Raids exist from 1.14; the API is newer than what we compile against, so use reflection. */
    private static void addRaids(World world, List<Source> out) {
        try {
            Method getRaids = world.getClass().getMethod("getRaids");
            Object raids = getRaids.invoke(world);
            if (!(raids instanceof Collection)) {
                return;
            }
            for (Object raid : (Collection<?>) raids) {
                Object loc = raid.getClass().getMethod("getLocation").invoke(raid);
                if (loc instanceof Location) {
                    Location l = (Location) loc;
                    out.add(Source.sphere(RangeType.RAID, l.getX(), l.getY(), l.getZ(), RangeMath.RAID_RANGE, null));
                }
            }
        } catch (Throwable ignored) {
            // No raids on this version.
        }
    }

    static int[] spawnChunks(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return null;
        }
        LogicVersion v = LogicVersion.current();
        int[] version = {v.major, v.minor, v.patch};
        int radius = 2;
        if (v.isAtLeast(1, 20, 5)) {
            try {
                String value = world.getGameRuleValue("spawnChunkRadius");
                if (value != null) {
                    radius = Integer.parseInt(value.trim());
                }
            } catch (Throwable ignored) {
                // Keep the default.
            }
        } else {
            try {
                if (!world.getKeepSpawnInMemory()) {
                    return null;
                }
            } catch (Throwable ignored) {
                // Assume kept.
            }
        }
        Location spawn = world.getSpawnLocation();
        return RangeMath.spawnChunks(version, spawn.getBlockX(), spawn.getBlockZ(), radius);
    }

    private static final class Scan {
        final String world;
        final long time;
        final List<Source> sources;

        Scan(String world, long time, List<Source> sources) {
            this.world = world;
            this.time = time;
            this.sources = sources;
        }
    }

    enum Kind { SQUARE, SPHERE, BOX }

    static final class Source {
        RangeType type;
        Kind kind;
        double x1, z1, x2, z2, minY, maxY;
        double cx, cy, cz, radius;
        Color color;

        static Source square(RangeType type, double x1, double z1, double x2, double z2, double minY) {
            Source s = new Source();
            s.type = type;
            s.kind = Kind.SQUARE;
            s.x1 = x1;
            s.z1 = z1;
            s.x2 = x2;
            s.z2 = z2;
            s.minY = minY;
            s.cx = (x1 + x2) / 2;
            s.cy = minY;
            s.cz = (z1 + z2) / 2;
            return s;
        }

        static Source sphere(RangeType type, double cx, double cy, double cz, double radius, Color color) {
            Source s = new Source();
            s.type = type;
            s.kind = Kind.SPHERE;
            s.cx = cx;
            s.cy = cy;
            s.cz = cz;
            s.radius = radius;
            s.color = color;
            return s;
        }

        static Source box(RangeType type, double x1, double y1, double z1, double x2, double y2, double z2) {
            Source s = new Source();
            s.type = type;
            s.kind = Kind.BOX;
            s.x1 = x1;
            s.minY = y1;
            s.z1 = z1;
            s.x2 = x2;
            s.maxY = y2;
            s.z2 = z2;
            s.cx = (x1 + x2) / 2;
            s.cy = (y1 + y2) / 2;
            s.cz = (z1 + z2) / 2;
            return s;
        }

        @Override
        public String toString() {
            switch (kind) {
                case SQUARE:
                    return type.id + " square " + (int) Math.round((x2 - x1 - 1) / 2);
                case SPHERE:
                    return type.id + " sphere " + (int) radius;
                default:
                    return type.id + " box " + (int) Math.round((x2 - x1) / 2);
            }
        }

        double distanceSq(Location at) {
            double dx = cx - at.getX();
            double dy = cy - at.getY();
            double dz = cz - at.getZ();
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
