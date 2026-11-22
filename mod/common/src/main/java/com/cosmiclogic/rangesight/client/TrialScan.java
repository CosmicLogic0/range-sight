package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Trial spawner and vault ranges (Minecraft 1.21+). Older versions get an empty copy of this class, so the rest of
 * the code never needs to know whether these blocks exist.
 */
final class TrialScan {

    static final boolean SUPPORTED = true;

    private TrialScan() {
    }

    /** Adds the range of a trial spawner or vault; returns true if {@code be} was one. */
    static boolean add(BlockEntity be, RangeConfig config, List<RangeShape> out) {
        boolean spawner = be instanceof TrialSpawnerBlockEntity;
        boolean vault = be instanceof VaultBlockEntity;
        if (!spawner && !vault) {
            return false;
        }
        if (!config.isEnabled(RangeType.TRIAL)) {
            return true;
        }
        BlockPos p = be.getBlockPos();
        double x = p.getX() + 0.5;
        double y = p.getY() + 0.5;
        double z = p.getZ() + 0.5;
        double range = spawner
                ? number(call(call(be, "getTrialSpawner"), "getRequiredPlayerRange"), RangeMath.TRIAL_SPAWNER_DEFAULT_RANGE)
                : number(call(call(be, "getConfig"), "activationRange"), RangeMath.VAULT_DEFAULT_RANGE);
        out.add(RangeShape.sphere(RangeType.TRIAL, config.color(RangeType.TRIAL), x, y, z, range));
        return true;
    }

    /** Calls a no-argument method by name (names differ a little between 1.21.x versions), or returns null. */
    private static Object call(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number && ((Number) value).doubleValue() > 0 ? ((Number) value).doubleValue() : fallback;
    }
}
