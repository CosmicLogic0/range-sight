package com.cosmiclogic.rangesight;

/**
 * The game's own range rules, as plain math (no Bukkit), so they can be unit tested.
 * Same numbers as vanilla Minecraft.
 */
public final class RangeMath {

    private RangeMath() {
    }

    /** Beacon effect reach in blocks for a pyramid of 1-4 levels (0 = not active). */
    public static int beaconRange(int levels) {
        return levels <= 0 ? 0 : levels * 10 + 10;
    }

    /** Conduit reach in blocks for the number of prismarine/sea lantern frame blocks (under 16 = not active). */
    public static int conduitRange(int frameBlocks) {
        return frameBlocks < 16 ? 0 : frameBlocks / 7 * 16;
    }

    /** True if (dx, dy, dz) from the conduit is one of the 42 frame positions in vanilla. */
    public static boolean isConduitFramePosition(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        if (ax > 2 || ay > 2 || az > 2) {
            return false;
        }
        if (ax <= 1 && ay <= 1 && az <= 1) {
            return false;
        }
        return dx == 0 && (ay == 2 || az == 2)
                || dy == 0 && (ax == 2 || az == 2)
                || dz == 0 && (ax == 2 || ay == 2);
    }

    /** Sculk sensor listening radius (calibrated sculk sensors hear twice as far). */
    public static int sculkSensorRange(boolean calibrated) {
        return calibrated ? 16 : 8;
    }

    /** Sculk shrieker: how far it hears sculk sensors. */
    public static final int SHRIEKER_LISTEN_RANGE = 8;
    /** Sculk shrieker: players within this distance get Darkness when it warns. */
    public static final int SHRIEKER_DARKNESS_RANGE = 40;
    /** Raids: players within this distance of the raid center are part of the raid. */
    public static final int RAID_RANGE = 96;
    /** Monster spawner defaults (servers can change them per spawner). */
    public static final int SPAWNER_DEFAULT_PLAYER_RANGE = 16;
    public static final int SPAWNER_DEFAULT_SPAWN_RANGE = 4;

    /**
     * Radius of a sphere's slice at a height {@code dy} away from its center (0 if outside).
     * Used to draw a sphere as one circle at the player's height.
     */
    public static double sliceRadius(double sphereRadius, double dy) {
        double d = sphereRadius * sphereRadius - dy * dy;
        return d <= 0 ? 0 : Math.sqrt(d);
    }

    /**
     * Spawn chunk square, in chunk coordinates {minX, minZ, maxX, maxZ}, for the entity-processing area.
     * Returns null when the version has no spawn chunks (1.21.9+) or the radius is 0.
     *
     * @param version      {major, minor, patch} of the server, e.g. {1, 20, 1} or {26, 1, 2}
     * @param spawnX       world spawn block x
     * @param spawnZ       world spawn block z
     * @param gameruleRadius spawnChunkRadius game rule (only used on 1.20.5 to 1.21.8)
     */
    public static int[] spawnChunks(int[] version, int spawnX, int spawnZ, int gameruleRadius) {
        int cx = Math.floorDiv(spawnX, 16);
        int cz = Math.floorDiv(spawnZ, 16);
        if (atLeast(version, 1, 21, 9)) {
            return null;
        }
        if (atLeast(version, 1, 20, 5)) {
            int r = gameruleRadius - 1;
            if (gameruleRadius <= 0) {
                return null;
            }
            return new int[]{cx - r, cz - r, cx + r, cz + r};
        }
        if (atLeast(version, 1, 14, 0)) {
            return new int[]{cx - 9, cz - 9, cx + 9, cz + 9};
        }
        // 1.13 and older: chunks whose middle is within 128 blocks of spawn on both axes.
        int minX = (int) Math.ceil((spawnX - 128 - 8) / 16.0);
        int maxX = Math.floorDiv(spawnX + 128 - 8, 16);
        int minZ = (int) Math.ceil((spawnZ - 128 - 8) / 16.0);
        int maxZ = Math.floorDiv(spawnZ + 128 - 8, 16);
        return new int[]{minX, minZ, maxX, maxZ};
    }

    static boolean atLeast(int[] v, int major, int minor, int patch) {
        if (v[0] != major) {
            return v[0] > major;
        }
        if (v[1] != minor) {
            return v[1] > minor;
        }
        return v[2] >= patch;
    }
}
