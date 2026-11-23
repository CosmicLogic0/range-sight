package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * What the settings screen shows, without any screen code, so every Minecraft version (each with its own button
 * classes) builds the same screen: one row per range type (on/off and color), then All on/All off, line width,
 * search distance and Done. Coordinates are relative to the screen's center and top row.
 */
final class SettingsRows {

    static final int ROW = 22;

    /** One button: where it goes, what it says, what it does. */
    static final class Spec {
        final int x;
        final int row;
        final int width;
        private final java.util.function.Supplier<String> label;
        /** Text color (0xRRGGBB) or -1 for the normal color. */
        private final java.util.function.IntSupplier color;
        final String tip;
        final Runnable press;

        Spec(int x, int row, int width, java.util.function.Supplier<String> label, java.util.function.IntSupplier color,
             String tip, Runnable press) {
            this.x = x;
            this.row = row;
            this.width = width;
            this.label = label;
            this.color = color;
            this.tip = tip;
            this.press = press;
        }

        String label() {
            return label.get();
        }

        int color() {
            return color.getAsInt();
        }
    }

    private SettingsRows() {
    }

    static String title() {
        return I18n.get(RangeSight.MOD_ID + ".settings.title");
    }

    /** The help line under the buttons. */
    static String keysLine() {
        return I18n.get(RangeSight.MOD_ID + ".settings.keys");
    }

    /** Number of rows, for centering the screen. */
    static int rows(int[] mc) {
        return (RangeType.availableTypes(mc).size() + 1) / 2 + 3;
    }

    static List<Spec> build(RangeConfig config, int[] mc, Runnable closer) {
        List<Spec> out = new ArrayList<>();
        List<RangeType> types = RangeType.availableTypes(mc);
        String id = RangeSight.MOD_ID;
        for (int i = 0; i < types.size(); i++) {
            RangeType type = types.get(i);
            int col = i % 2 == 0 ? -205 : 5;
            int row = i / 2;
            out.add(new Spec(col, row, 150,
                    () -> I18n.get(id + (config.isEnabled(type) ? ".settings.on" : ".settings.off"), I18n.get(type.labelKey())),
                    () -> config.isEnabled(type) ? -1 : 0xA0A0A0,
                    id + ".help." + type.id,
                    () -> config.setEnabled(type, !config.isEnabled(type))));
            out.add(new Spec(col + 152, row, 48,
                    () -> I18n.get(id + ".settings.color"),
                    () -> config.color(type) & 0xFFFFFF,
                    id + ".settings.color.tip",
                    () -> config.nextColor(type)));
        }
        int row = (types.size() + 1) / 2;
        out.add(new Spec(-205, row, 98, () -> I18n.get(id + ".settings.all_on"), () -> -1, null, () -> config.setAll(true)));
        out.add(new Spec(-103, row, 98, () -> I18n.get(id + ".settings.all_off"), () -> -1, null, () -> config.setAll(false)));
        out.add(new Spec(5, row, 200, () -> I18n.get(id + ".settings.width", Math.round(config.lineWidth)), () -> -1,
                id + ".settings.width.tip", config::nextLineWidth));
        out.add(new Spec(-205, row + 1, 410, () -> I18n.get(id + ".settings.distance", config.scanChunks), () -> -1,
                id + ".settings.distance.tip", config::nextScanChunks));
        out.add(new Spec(-100, row + 2, 200, () -> I18n.get("gui.done"), () -> -1, null, closer));
        return out;
    }

    /** Tooltip text for a button, or null. */
    static String tip(Spec spec) {
        return spec.tip == null ? null : I18n.get(spec.tip);
    }
}
