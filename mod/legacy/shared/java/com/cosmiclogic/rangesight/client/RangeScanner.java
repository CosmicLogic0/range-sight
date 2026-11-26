package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finds range blocks in loaded chunks around the player (older versions; differences go through {@link Mc}). */
public final class RangeScanner {


    private static final Set<Block> CONDUIT_FRAME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE)));

    private RangeScanner() {
    }

    public static List<RangeShape> scan(Minecraft mc, RangeConfig config) {
        List<RangeShape> shapes = new ArrayList<>();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !config.anyEnabled()) {
            return shapes;
        }
        final Vec3 at = mc.player.position();
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int r = config.scanChunks;

        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    addBlockEntity(level, be, config, shapes);
                }
            }
        }
        if (config.isEnabled(RangeType.RAID)) {
            addRaid(mc, level, config, shapes);
        }
        if (config.isEnabled(RangeType.SPAWN_CHUNKS)) {
            addSpawnChunks(mc, level, config, shapes);
        }
        shapes.sort(Comparator.comparingDouble(s -> s.distanceSq(at.x, at.y, at.z)));
        return shapes; // RangeClient keeps the nearest config.maxOutlines (and counts all of them for the HUD)
    }

    private static void addBlockEntity(ClientLevel level, BlockEntity be, RangeConfig config, List<RangeShape> out) {
        if (TrialScan.add(be, config, out)) {
            return; // trial spawner or vault (1.21+)
        }
        BlockPos p = be.getBlockPos();
        double x = p.getX() + 0.5;
        double y = p.getY() + 0.5;
        double z = p.getZ() + 0.5;

        if (be instanceof BeaconBlockEntity) {
            if (config.isEnabled(RangeType.BEACON)) {
                int range = RangeMath.beaconRange(beaconLevels(level, p));
                if (range > 0) {
                    out.add(RangeShape.column(RangeType.BEACON, config.color(RangeType.BEACON),
                            p.getX() - range, p.getZ() - range, p.getX() + 1 + range, p.getZ() + 1 + range,
                            p.getY() - range, Mc.maxY(level)));
                }
            }
        } else if (be instanceof ConduitBlockEntity) {
            if (config.isEnabled(RangeType.CONDUIT)) {
                int range = RangeMath.conduitRange(conduitFrames(level, p));
                if (range > 0) {
                    out.add(RangeShape.sphere(RangeType.CONDUIT, config.color(RangeType.CONDUIT), x, y, z, range));
                }
            }
        } else if (be instanceof SpawnerBlockEntity) {
            if (config.isEnabled(RangeType.SPAWNER)) {
                int[] ranges = Mc.spawnerRanges((SpawnerBlockEntity) be);
                int color = config.color(RangeType.SPAWNER);
                out.add(RangeShape.sphere(RangeType.SPAWNER, color, x, y, z, ranges[0]));
                out.add(RangeShape.box(RangeType.SPAWNER, config.spawnBoxColor(), x - ranges[1], p.getY() - 1, z - ranges[1],
                        x + ranges[1], p.getY() + 2, z + ranges[1]));
            }
        } else if (config.isEnabled(RangeType.SCULK)) {
            // 8 = sculk sensor / shrieker listening, 16 = calibrated sensor; shriekers also warn players within 40
            int range = Mc.sculkRange(be);
            if (range != 0) {
                int color = config.color(RangeType.SCULK);
                out.add(RangeShape.sphere(RangeType.SCULK, color, x, y, z, Math.abs(range)));
                if (range < 0) {
                    out.add(RangeShape.sphere(RangeType.SCULK, config.darknessColor(), x, y, z, RangeMath.SHRIEKER_DARKNESS_RANGE));
                }
            }
        }
    }

    static int beaconLevels(Level level, BlockPos pos) {
        int levels = 0;
        for (int l = 1; l <= 4; l++) {
            int y = pos.getY() - l;
            if (y < Mc.minY(level)) {
                break;
            }
            for (int x = pos.getX() - l; x <= pos.getX() + l; x++) {
                for (int z = pos.getZ() - l; z <= pos.getZ() + l; z++) {
                    if (!Mc.isBeaconBase(level.getBlockState(new BlockPos(x, y, z)))) {
                        return levels;
                    }
                }
            }
            levels = l;
        }
        return levels;
    }

    static int conduitFrames(Level level, BlockPos pos) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (RangeMath.isConduitFramePosition(dx, dy, dz)
                            && CONDUIT_FRAME.contains(level.getBlockState(new BlockPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)).getBlock())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Raid centers are only known to the server, so this works in single player and on LAN worlds you host. */
    private static void addRaid(Minecraft mc, ClientLevel level, RangeConfig config, List<RangeShape> out) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        try {
            ServerLevel serverLevel = server.getLevel(level.dimension());
            if (serverLevel == null) {
                return;
            }
            int reach = RangeMath.RAID_RANGE * 2;
            Raid raid = serverLevel.getRaids().getNearbyRaid(mc.player.blockPosition(), reach * reach);
            if (raid != null) {
                BlockPos c = raid.getCenter();
                out.add(RangeShape.sphere(RangeType.RAID, config.color(RangeType.RAID),
                        c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, RangeMath.RAID_RANGE));
            }
        } catch (RuntimeException ignored) {
            // The server was busy changing raids; try again next scan.
        }
    }

    private static void addSpawnChunks(Minecraft mc, ClientLevel level, RangeConfig config, List<RangeShape> out) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        BlockPos spawn = Mc.spawnPos(level);
        int[] chunks = RangeMath.spawnChunks(Mc.VERSION, spawn.getX(), spawn.getZ(), 2);
        if (chunks != null) {
            out.add(RangeShape.column(RangeType.SPAWN_CHUNKS, config.color(RangeType.SPAWN_CHUNKS),
                    chunks[0] * 16, chunks[1] * 16, (chunks[2] + 1) * 16, (chunks[3] + 1) * 16,
                    Mc.minY(level), Mc.maxY(level)));
        }
    }

}
