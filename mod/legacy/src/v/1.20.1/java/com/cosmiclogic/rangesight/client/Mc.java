package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeType;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Minecraft calls that differ between versions. This copy: 1.20.1. */
final class Mc {

    static final int[] VERSION = {1, 20, 1};

    private Mc() {
    }

    static KeyMapping newKey(String name, int key) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, RangeType.KEY_CATEGORY);
    }

    /** A chat line (not the action bar) from a translation key with one text argument. */
    static void chat(Minecraft mc, String key, String arg, boolean warning) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key, Component.literal(arg).withStyle(warning ? ChatFormatting.WHITE : ChatFormatting.YELLOW))
                    .withStyle(warning ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
        }
    }

    /** The key's current binding as text, e.g. "Num 0". */
    static String keyText(KeyMapping key) {
        return key.getTranslatedKeyMessage().getString();
    }

    static void say(Minecraft mc, String labelKey, boolean on, int labelRgb, int stateRgb) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.empty()
                    .append(Component.translatable(labelKey).withStyle(st -> st.withColor(TextColor.fromRgb(labelRgb))))
                    .append(Component.literal(": "))
                    .append(Component.translatable(on ? "options.on" : "options.off").withStyle(st -> st.withColor(TextColor.fromRgb(stateRgb)))), true);
        }
    }

    static int minY(Level level) {
        return level.getMinBuildHeight();
    }

    static int maxY(Level level) {
        return level.getMaxBuildHeight();
    }

    static boolean isBeaconBase(BlockState state) {
        return state.is(BlockTags.BEACON_BASE_BLOCKS);
    }

    /** {activation range, spawn area range}. */
    static int[] spawnerRanges(SpawnerBlockEntity spawner) {
        int[] out = {16, 4};
        try {
            CompoundTag tag = spawner.getSpawner().save(new CompoundTag());
            if (tag.contains("RequiredPlayerRange")) {
                out[0] = tag.getShort("RequiredPlayerRange");
            }
            if (tag.contains("SpawnRange")) {
                out[1] = tag.getShort("SpawnRange");
            }
        } catch (RuntimeException ignored) {
            // keep the defaults
        }
        return out;
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

    static BlockPos spawnPos(ClientLevel level) {
        return level.getSharedSpawnPos();
    }

    static int renderDistance(Minecraft mc) {
        return mc.options.getEffectiveRenderDistance();
    }

    static void lineWidth(float width) {
        RenderSystem.lineWidth(width);
    }

    static RenderType lineType() {
        return RenderType.lines();
    }

    static void line(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float nx, float ny, float nz, int argb, float width) {
        vc.vertex(p.pose(), x, y, z).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).normal(p.normal(), nx, ny, nz).endVertex();
    }
}
