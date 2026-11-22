package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Finds range blocks in loaded chunks around the player and turns them into outlines.
 * Runs once a second (not every frame); the renderer only draws the result.
 */
public final class RangeScanner {

    /** The Minecraft version this build is for (spawn chunk rules changed over time). */
    static final int[] MC_VERSION = {26, 0, 0};

    /** Never keep more than this many outlines, so huge bases stay smooth. */
    public static final int MAX_SHAPES = 48;

    private static final Set<Block> CONDUIT_FRAME = Set.of(
            Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE);

    private RangeScanner() {
    }

    public static List<RangeShape> scan(Minecraft mc, RangeConfig config) {
        List<RangeShape> shapes = new ArrayList<>();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !config.anyEnabled()) {
            return shapes;
        }
        Vec3 at = mc.player.position();
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
        return shapes.size() > MAX_SHAPES ? new ArrayList<>(shapes.subList(0, MAX_SHAPES)) : shapes;
    }

    private static void addBlockEntity(ClientLevel level, BlockEntity be, RangeConfig config, List<RangeShape> out) {
        if (TrialScan.add(be, config, out)) {
            return; // trial spawner or vault (1.21+)
        }
        BlockPos p = be.getBlockPos();
        double x = p.getX() + 0.5;
        double y = p.getY() + 0.5;
        double z = p.getZ() + 0.5;

        if (be instanceof BeaconBlockEntity && config.isEnabled(RangeType.BEACON)) {
            int range = RangeMath.beaconRange(beaconLevels(level, p));
            if (range > 0) {
                // Vanilla: from beacon - range to beacon + 1 + range, and from y - range up to the top of the world.
                out.add(RangeShape.column(RangeType.BEACON, config.color(RangeType.BEACON),
                        p.getX() - range, p.getZ() - range, p.getX() + 1 + range, p.getZ() + 1 + range,
                        p.getY() - range, level.getMaxY()));
            }
        } else if (be instanceof ConduitBlockEntity && config.isEnabled(RangeType.CONDUIT)) {
            int range = RangeMath.conduitRange(conduitFrames(level, p));
            if (range > 0) {
                out.add(RangeShape.sphere(RangeType.CONDUIT, config.color(RangeType.CONDUIT), x, y, z, range));
            }
        } else if (be instanceof SpawnerBlockEntity spawner && config.isEnabled(RangeType.SPAWNER)) {
            int playerRange = RangeMath.SPAWNER_DEFAULT_PLAYER_RANGE;
            int spawnRange = RangeMath.SPAWNER_DEFAULT_SPAWN_RANGE;
            // 26.x isn't obfuscated, so the spawner's own fields can be read by name.
            playerRange = intField(spawner.getSpawner(), "requiredPlayerRange", playerRange);
            spawnRange = intField(spawner.getSpawner(), "spawnRange", spawnRange);
            int color = config.color(RangeType.SPAWNER);
            out.add(RangeShape.sphere(RangeType.SPAWNER, color, x, y, z, playerRange));
            out.add(RangeShape.box(RangeType.SPAWNER, color, x - spawnRange, p.getY() - 1, z - spawnRange,
                    x + spawnRange, p.getY() + 2, z + spawnRange));
        } else if (config.isEnabled(RangeType.SCULK)) {
            int color = config.color(RangeType.SCULK);
            if (be instanceof CalibratedSculkSensorBlockEntity) {
                out.add(RangeShape.sphere(RangeType.SCULK, color, x, y, z, RangeMath.sculkSensorRange(true)));
            } else if (be instanceof SculkSensorBlockEntity) {
                out.add(RangeShape.sphere(RangeType.SCULK, color, x, y, z, RangeMath.sculkSensorRange(false)));
            } else if (be instanceof SculkShriekerBlockEntity) {
                out.add(RangeShape.sphere(RangeType.SCULK, color, x, y, z, RangeMath.SHRIEKER_LISTEN_RANGE));
                out.add(RangeShape.sphere(RangeType.SCULK, darker(color), x, y, z, RangeMath.SHRIEKER_DARKNESS_RANGE));
            }
        }
    }

    static int beaconLevels(Level level, BlockPos pos) {
        int levels = 0;
        for (int l = 1; l <= 4; l++) {
            int y = pos.getY() - l;
            if (y < level.getMinY()) {
                break;
            }
            for (int x = pos.getX() - l; x <= pos.getX() + l; x++) {
                for (int z = pos.getZ() - l; z <= pos.getZ() + l; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
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
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (RangeMath.isConduitFramePosition(dx, dy, dz)
                            && CONDUIT_FRAME.contains(level.getBlockState(m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)).getBlock())) {
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

    /** Mojang removed spawn chunks in 1.21.9, so 26.x has none to show. */
    private static void addSpawnChunks(Minecraft mc, ClientLevel level, RangeConfig config, List<RangeShape> out) {
    }

    private static int intField(Object o, String name, int fallback) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return ((Number) f.get(o)).intValue();
            } catch (NoSuchFieldException e) {
                // look in the parent class
            } catch (ReflectiveOperationException | RuntimeException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int darker(int argb) {
        int r = (argb >> 16 & 0xFF) * 3 / 5;
        int g = (argb >> 8 & 0xFF) * 3 / 5;
        int b = (argb & 0xFF) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
