package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

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
        // 1.21.9+: key categories are registered objects. The keyboard type is the first one (KEYSYM, later KEYBOARD).
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(RangeSight.MOD_ID, "main"));
        InputConstants.Type keyboard = InputConstants.Type.values()[0];
        for (RangeType type : RangeType.availableTypes(RangeScanner.MC_VERSION)) {
            TYPE_KEYS.put(type, new KeyMapping(type.keyName(), keyboard, type.defaultKey, category));
        }
        allKey = new KeyMapping(RangeType.KEY_ALL, keyboard, RangeType.KEY_ALL_DEFAULT, category);
        settingsKey = new KeyMapping(RangeType.KEY_SETTINGS, keyboard, RangeType.KEY_SETTINGS_DEFAULT, category);
        hudKey = new KeyMapping(RangeType.KEY_HUD, keyboard, RangeType.KEY_HUD_DEFAULT, category);
        RangeSight.LOG.info("Range Sight client ready ({} range types on)", countEnabled());
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
                say(mc, Component.translatable(type.labelKey()), on);
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
            say(mc, Component.translatable(RangeSight.MOD_ID + ".all"), on);
            Features.switched(mc, config, on);
            changed = true;
        }
        while (hudKey.consumeClick()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
            say(mc, Component.translatable(RangeType.KEY_HUD), config.hudEnabled);
            Features.switched(mc, config, config.hudEnabled);
        }
        if (mc.level == null || mc.player == null) {
            shapes = Collections.emptyList();
            Features.clearStats();
            ticks = Integer.MAX_VALUE - 1; // scan right away when a world opens
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
                RangeSight.LOG.info("Range Sight drawing {} outlines", shapes.size());
            }
        }
        Features.tickParticles(mc, config, shapes, mc.player.getY());
    }

    /** Called by the loader while Minecraft collects the frame's gizmos (outlines). */
    public static void onGizmos() {
        Minecraft mc = Minecraft.getInstance();
        List<RangeShape> current = shapes;
        if (current.isEmpty() || mc.player == null || config == null || !Features.lines(config)) {
            return;
        }
        RangeRenderer.render(current, config.lineWidth, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            RangeSight.LOG.info("Range Sight rendered its first frame of outlines");
        }
    }

    /** Scan right now (used by the automatic test) and return the outlines. */
    /** Settings changed: scan again on the next tick. */
    static void rescanSoon() {
        ticks = Integer.MAX_VALUE - 1;
    }

    /** The Minecraft version this copy of Range Sight is built for. */
    static int[] mcVersion() {
        return RangeScanner.MC_VERSION;
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

    /** Once per install: how to use Range Sight. Once per broken config: where the old file went. */
    private static void showHints(Minecraft mc) {
        if (!config.hintShown && config.tip && mc.player.tickCount > 40) {
            config.hintShown = true;
            config.save();
            mc.player.sendSystemMessage(Component.translatable(RangeSight.MOD_ID + ".hint",
                    allKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
        }
        if (config.brokenFileNote != null && mc.player.tickCount > 40) {
            mc.player.sendSystemMessage(Component.translatable(RangeSight.MOD_ID + ".broken_config",
                    Component.literal(config.brokenFileNote).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD));
            config.brokenFileNote = null;
        }
        if (!config.badValues.isEmpty() && mc.player.tickCount > 40) {
            mc.player.sendSystemMessage(Component.translatable(RangeSight.MOD_ID + ".bad_values",
                    Component.literal(String.join(", ", config.badValues)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD));
            config.badValues.clear();
        }
    }

    /** CI tests only. */
    static RangeConfig config() {
        return config;
    }

    private static void say(Minecraft mc, Component what, boolean on) {
        if (mc.player != null && config.messages) {
            int label = config.messageColor & 0xFFFFFF;
            int state = (on ? config.messageOnColor : config.messageOffColor) & 0xFFFFFF;
            mc.player.sendOverlayMessage(Component.empty()
                    .append(what.copy().withStyle(st -> st.withColor(TextColor.fromRgb(label))))
                    .append(Component.literal(": "))
                    .append(Component.translatable(on ? "options.on" : "options.off")
                            .withStyle(st -> st.withColor(TextColor.fromRgb(state)))));
        }
    }
}
