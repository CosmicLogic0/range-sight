package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of Range Sight, shared by every loader: keys, scanning once a second, drawing every frame.
 * Loaders only register {@link #keys()} and forward the tick and render events.
 */
public final class RangeClient {

    private static final int SCAN_EVERY_TICKS = 20;

    private static RangeConfig config;
    private static final Map<RangeType, KeyMapping> TYPE_KEYS = new EnumMap<>(RangeType.class);
    private static KeyMapping allKey;
    private static volatile List<RangeShape> shapes = Collections.emptyList();
    private static int ticks = SCAN_EVERY_TICKS;
    private static boolean loggedFirstDraw;
    private static boolean loggedFirstFrame;

    private RangeClient() {
    }

    public static void init(Path configDir) {
        config = new RangeConfig(configDir);
        config.load();
        for (RangeType type : RangeType.values()) {
            TYPE_KEYS.put(type, new KeyMapping(type.keyName(), InputConstants.Type.KEYSYM, type.defaultKey, RangeType.KEY_CATEGORY));
        }
        allKey = new KeyMapping(RangeType.KEY_ALL, InputConstants.Type.KEYSYM, RangeType.KEY_ALL_DEFAULT, RangeType.KEY_CATEGORY);
        RangeSight.LOG.info("Range Sight client ready ({} range types on)", countEnabled());
    }

    /** All key mappings, for the loader to register. */
    public static List<KeyMapping> keys() {
        List<KeyMapping> list = new ArrayList<>(TYPE_KEYS.values());
        list.add(allKey);
        return list;
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (config == null) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<RangeType, KeyMapping> entry : TYPE_KEYS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                RangeType type = entry.getKey();
                boolean on = !config.isEnabled(type);
                config.setEnabled(type, on);
                say(mc, Component.translatable(type.labelKey()), on);
                changed = true;
            }
        }
        while (allKey.consumeClick()) {
            boolean on = !config.anyEnabled();
            config.setAll(on);
            say(mc, Component.translatable(RangeSight.MOD_ID + ".all"), on);
            changed = true;
        }
        if (mc.level == null || mc.player == null) {
            shapes = Collections.emptyList();
            ticks = SCAN_EVERY_TICKS; // scan right away when a world opens
            return;
        }
        ClientSelfTest.tick(mc, config);
        if (changed || ++ticks >= SCAN_EVERY_TICKS) {
            ticks = 0;
            shapes = config.anyEnabled() ? RangeScanner.scan(mc, config) : Collections.emptyList();
            if (!loggedFirstDraw && !shapes.isEmpty()) {
                loggedFirstDraw = true;
                RangeSight.LOG.info("Range Sight drawing {} outlines", shapes.size());
            }
        }
    }

    public static void onRender(PoseStack poseStack, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        List<RangeShape> current = shapes;
        if (current.isEmpty() || mc.player == null || config == null) {
            return;
        }
        RangeRenderer.render(poseStack, camera, current, config.lineWidth, mc.player.getY());
        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            RangeSight.LOG.info("Range Sight rendered its first frame of outlines");
        }
    }

    /** Scan right now (used by the automatic test) and return the outlines. */
    static List<RangeShape> rescanNow() {
        Minecraft mc = Minecraft.getInstance();
        shapes = config.anyEnabled() ? RangeScanner.scan(mc, config) : Collections.emptyList();
        return shapes;
    }

    /** Number of outlines currently drawn (used by the automatic tests). */
    public static int shapeCount() {
        return shapes.size();
    }

    private static int countEnabled() {
        int n = 0;
        for (RangeType type : RangeType.values()) {
            if (config.isEnabled(type)) {
                n++;
            }
        }
        return n;
    }

    private static void say(Minecraft mc, Component what, boolean on) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.empty()
                    .append(what.copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": "))
                    .append(Component.translatable(on ? "options.on" : "options.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), true);
        }
    }
}
