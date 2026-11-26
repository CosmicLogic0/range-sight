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

    private static RangeConfig config;
    private static final Map<RangeType, KeyMapping> TYPE_KEYS = new EnumMap<>(RangeType.class);
    private static KeyMapping allKey;
    private static KeyMapping settingsKey;
    private static KeyMapping hudKey;
    private static volatile List<RangeShape> shapes = Collections.emptyList();
    /** Ticks since the last scan; starts "due" so the first scan happens right away. */
    private static int ticks = Integer.MAX_VALUE - 1;
    /** The world the outlines belong to; a new world or dimension clears them right away. */
    private static Object shapesLevel;
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
        settingsKey = Mc.newKey(RangeType.KEY_SETTINGS, RangeType.KEY_SETTINGS_DEFAULT);
        hudKey = Mc.newKey(RangeType.KEY_HUD, RangeType.KEY_HUD_DEFAULT);
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
        list.add(settingsKey);
        list.add(hudKey);
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
                say(mc, type.labelKey(), on);
                Features.switched(mc, config, on);
                changed = true;
            }
        }
        while (settingsKey.consumeClick()) {
            if (Screens.current(mc) == null) {
                Screens.open(mc, new SettingsScreen(null));
            }
        }
        while (allKey.consumeClick()) {
            boolean on = !config.anyEnabled();
            config.setAll(on);
            say(mc, RangeSight.MOD_ID + ".all", on);
            Features.switched(mc, config, on);
            changed = true;
        }
        while (hudKey.consumeClick()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
            say(mc, RangeType.KEY_HUD, config.hudEnabled);
            Features.switched(mc, config, config.hudEnabled);
        }
        if (mc.level == null || mc.player == null) {
            shapes = Collections.emptyList();
            Features.clearStats();
            ticks = Integer.MAX_VALUE - 1;
            return;
        }
        if (mc.level != shapesLevel) {
            shapesLevel = mc.level;
            shapes = Collections.emptyList(); // never draw the old dimension's outlines in the new one
            Features.clearStats();
            ticks = Integer.MAX_VALUE - 1;
        }
        showHints(mc);
        ClientSelfTest.tick(mc, config);
        if (changed || ++ticks >= config.updateTicks) {
            ticks = 0;
            scanNow(mc);
            if (!loggedFirstDraw && !shapes.isEmpty()) {
                loggedFirstDraw = true;
                RangeSight.LOG.info("Range Sight drawing " + shapes.size() + " outlines");
            }
        }
        Features.tickParticles(mc, config, shapes, mc.player.getY());
    }

    private static void say(Minecraft mc, String labelKey, boolean on) {
        if (config.messages) {
            Mc.say(mc, labelKey, on, config.messageColor & 0xFFFFFF, (on ? config.messageOnColor : config.messageOffColor) & 0xFFFFFF);
        }
    }

    /**
     * @param poseStack the level pose stack with the camera rotation applied (not the camera position)
     * @param camera    camera position in the world
     */
    public static void onRender(PoseStack poseStack, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        List<RangeShape> current = shapes;
        if (current.isEmpty() || mc.player == null || config == null || !Features.lines(config)) {
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
        if (!config.hintShown && config.tip && mc.player.tickCount > 40) {
            config.hintShown = true;
            config.save();
            Mc.chat(mc, RangeSight.MOD_ID + ".hint", Mc.keyText(allKey), false);
        }
        if (config.brokenFileNote != null && mc.player.tickCount > 40) {
            Mc.chat(mc, RangeSight.MOD_ID + ".broken_config", config.brokenFileNote, true);
            config.brokenFileNote = null;
        }
        if (!config.badValues.isEmpty() && mc.player.tickCount > 40) {
            Mc.chat(mc, RangeSight.MOD_ID + ".bad_values", String.join(", ", config.badValues), true);
            config.badValues.clear();
        }
    }

    /** CI tests only. */
    static RangeConfig config() {
        return config;
    }

    /** Scan right now (used by the automatic test) and return the outlines. */
    /** Settings changed: scan again on the next tick. */
    static void rescanSoon() {
        ticks = Integer.MAX_VALUE - 1;
    }

    /** The Minecraft version this copy of Range Sight is built for. */
    static int[] mcVersion() {
        return Mc.VERSION;
    }

    /** The outlines drawn right now (automatic test). */
    static List<RangeShape> current() {
        return shapes;
    }

    static List<RangeShape> rescanNow() {
        scanNow(Minecraft.getInstance());
        return shapes;
    }

    private static void scanNow(Minecraft mc) {
        if (!config.anyEnabled()) {
            shapes = Collections.emptyList();
            Features.clearStats();
            return;
        }
        shapes = Features.afterScan(RangeScanner.scan(mc, config), config, mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    /** Called by the loader when the HUD is drawn; {@code context} is that version's drawing object. */
    public static void onHud(Object context) {
        HudDraw.draw(context);
    }

    /** HUD numbers from the last scan. */
    static RangeStats stats() {
        return Features.stats();
    }

    /** The range types this version has, in order. */
    static List<RangeType> types() {
        return RangeType.availableTypes(mcVersion());
    }
}
