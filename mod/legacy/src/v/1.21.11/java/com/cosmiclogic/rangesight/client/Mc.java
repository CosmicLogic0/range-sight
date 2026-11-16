package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Minecraft calls that differ between versions. This copy: 1.21.11. */
final class Mc {

    static final int[] VERSION = {1, 21, 11};

    private Mc() {
    }

    private static KeyMapping.Category category;

    static KeyMapping newKey(String name, int key) {
        if (category == null) {
            category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(RangeSight.MOD_ID, "main"));
        }
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, category);
    }

    static void say(Minecraft mc, String labelKey, boolean on) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.empty()
                    .append(Component.translatable(labelKey).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": "))
                    .append(Component.translatable(on ? "options.on" : "options.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), true);
        }
    }

    static int minY(Level level) {
        return level.getMinY();
    }

    static int maxY(Level level) {
        return level.getMaxY();
    }

    static boolean isBeaconBase(BlockState state) {
        return state.is(BlockTags.BEACON_BASE_BLOCKS);
    }

    /** {activation range, spawn area range}. */
    static int[] spawnerRanges(SpawnerBlockEntity spawner) {
        // Forge/NeoForge 1.21.11 run with Mojang's names, so the spawner's fields can be read by name.
        return new int[]{intField(spawner.getSpawner(), "requiredPlayerRange", 16), intField(spawner.getSpawner(), "spawnRange", 4)};
    }

    /** 0 = not sculk, 8 = sensor, 16 = calibrated sensor, -8 = shrieker (8 + Darkness ring). */
    static int sculkRange(BlockEntity be) {
        if (be instanceof CalibratedSculkSensorBlockEntity) {
            return 16;
        }
        if (be instanceof SculkSensorBlockEntity) {
            return 8;
        }
        if (be instanceof SculkShriekerBlockEntity) {
            return -8;
        }
        return 0;
    }

    /** No spawn chunks since 1.21.9, so the position doesn't matter. */
    static BlockPos spawnPos(ClientLevel level) {
        return BlockPos.ZERO;
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

    static int renderDistance(Minecraft mc) {
        return mc.options.getEffectiveRenderDistance();
    }

    /** 1.21.11: the width is set per line point (see {@link #line}). */
    static void lineWidth(float width) {
    }

    static RenderType lineType() {
        return RenderTypes.lines();
    }

    static void line(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float nx, float ny, float nz, int argb, float width) {
        vc.addVertex(p, x, y, z).setColor(argb).setNormal(p, nx, ny, nz).setLineWidth(width);
    }
}
