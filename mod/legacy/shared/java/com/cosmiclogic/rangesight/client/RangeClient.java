package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of Range Sight for the older versions (1.16.5 to 1.21.11): keys, scanning once a second,
 * drawing every frame. Everything that differs between versions goes through {@link Mc}.
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
        // Only the ranges this Minecraft version has get a key.
        for (RangeType type : RangeType.availableTypes(Mc.VERSION)) {
            TYPE_KEYS.put(type, Mc.newKey(type.keyName(), type.defaultKey));
        }
        allKey = Mc.newKey(RangeType.KEY_ALL, RangeType.KEY_ALL_DEFAULT);
        int on = 0;
        for (RangeType type : RangeType.values()) {
            if (config.isEnabled(type)) {
                on++;
            }
        }
        RangeSight.LOG.info("Range Sight client ready (" + on + " range types on)");
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
                Mc.say(mc, type.labelKey(), on);
                changed = true;
            }
        }
        while (allKey.consumeClick()) {
            boolean on = !config.anyEnabled();
            config.setAll(on);
            Mc.say(mc, RangeSight.MOD_ID + ".all", on);
            changed = true;
        }
        if (mc.level == null || mc.player == null) {
            shapes = Collections.emptyList();
            ticks = SCAN_EVERY_TICKS;
            return;
        }
        showHints(mc);
        ClientSelfTest.tick(mc, config);
        if (changed || ++ticks >= SCAN_EVERY_TICKS) {
            ticks = 0;
            shapes = config.anyEnabled() ? RangeScanner.scan(mc, config) : Collections.<RangeShape>emptyList();
            if (!loggedFirstDraw && !shapes.isEmpty()) {
                loggedFirstDraw = true;
                RangeSight.LOG.info("Range Sight drawing " + shapes.size() + " outlines");
            }
        }
    }

    /**
     * @param poseStack the level pose stack with the camera rotation applied (not the camera position)
     * @param camera    camera position in the world
     */
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

    /** Once per install: how to use Range Sight. Once per broken config: where the old file went. */
    private static void showHints(Minecraft mc) {
        if (!config.hintShown && mc.player.tickCount > 40) {
            config.hintShown = true;
            config.save();
            Mc.chat(mc, RangeSight.MOD_ID + ".hint", Mc.keyText(allKey), false);
        }
        if (config.brokenFileNote != null && mc.player.tickCount > 40) {
            Mc.chat(mc, RangeSight.MOD_ID + ".broken_config", config.brokenFileNote, true);
            config.brokenFileNote = null;
        }
    }

    /** CI tests only. */
    static RangeConfig config() {
        return config;
    }

    /** Scan right now (used by the automatic test) and return the outlines. */
    static List<RangeShape> rescanNow() {
        Minecraft mc = Minecraft.getInstance();
        shapes = config.anyEnabled() ? RangeScanner.scan(mc, config) : Collections.<RangeShape>emptyList();
        return shapes;
    }
}
