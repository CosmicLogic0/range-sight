package com.cosmiclogic.rangesight.paper;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Values from config.yml, clamped to safe ranges. Problems are collected so /range reload can show them. */
final class PluginSettings {

    int scanChunks = 4;
    int maxOutlines = 12;
    long rescanMs = 3000;
    boolean farParticles = true;
    final List<String> problems = new ArrayList<>();

    static PluginSettings from(FileConfiguration config) {
        PluginSettings s = new PluginSettings();
        s.scanChunks = clamp(config.getInt("scan-chunks", 4), 1, 8);
        s.maxOutlines = clamp(config.getInt("max-outlines", 12), 1, 40);
        s.rescanMs = clamp(config.getInt("rescan-seconds", 3), 1, 60) * 1000L;
        s.farParticles = config.getBoolean("far-particles", true);
        for (RangeType type : RangeType.values()) {
            String text = config.getString("colors." + type.id, null);
            Color color = RangeType.parseColor(text);
            if (text != null && color == null) {
                s.problems.add("colors." + type.id + ": '" + text + "' is not a #RRGGBB color, using the default");
            }
            type.color = color != null ? color : Color.fromRGB(type.defaultRgb);
        }
        return s;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
