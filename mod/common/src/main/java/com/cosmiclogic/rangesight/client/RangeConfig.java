package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * config/rangesight.json: which range types are on, and each type's color (#RRGGBB).
 * Missing keys get their defaults and are written back, so new options appear automatically.
 */
public final class RangeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Set<RangeType> enabled = EnumSet.noneOf(RangeType.class);
    private final Map<RangeType, Integer> colors = new EnumMap<>(RangeType.class);
    /** How far to look for range blocks, in chunks. */
    public int scanChunks = 6;
    /** Line width in pixels. */
    public float lineWidth = 2.0f;
    /** Automatic test only (CI): builds a beacon in single player and checks it is found. Never saved. */
    public boolean selfTest;

    public RangeConfig(Path configDir) {
        this.file = configDir.resolve(RangeSight.MOD_ID + ".json");
        for (RangeType type : RangeType.values()) {
            colors.put(type, type.defaultColor);
        }
    }

    public boolean isEnabled(RangeType type) {
        return enabled.contains(type);
    }

    public boolean anyEnabled() {
        return !enabled.isEmpty();
    }

    public void setEnabled(RangeType type, boolean on) {
        if (on) {
            enabled.add(type);
        } else {
            enabled.remove(type);
        }
        save();
    }

    public void setAll(boolean on) {
        enabled.clear();
        if (on) {
            enabled.addAll(EnumSet.allOf(RangeType.class));
        }
        save();
    }

    public int color(RangeType type) {
        return colors.get(type);
    }

    public void load() {
        if (Files.exists(file)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("scanChunks")) {
                    scanChunks = Math.max(1, Math.min(16, root.get("scanChunks").getAsInt()));
                }
                if (root.has("selfTest")) {
                    selfTest = root.get("selfTest").getAsBoolean();
                }
                if (root.has("lineWidth")) {
                    lineWidth = Math.max(1f, Math.min(10f, root.get("lineWidth").getAsFloat()));
                }
                JsonObject types = root.has("types") ? root.getAsJsonObject("types") : new JsonObject();
                for (RangeType type : RangeType.values()) {
                    if (!types.has(type.id)) {
                        continue;
                    }
                    JsonObject t = types.getAsJsonObject(type.id);
                    if (t.has("enabled") && t.get("enabled").getAsBoolean()) {
                        enabled.add(type);
                    }
                    if (t.has("color")) {
                        colors.put(type, parseColor(t.get("color").getAsString(), type.defaultColor));
                    }
                }
            } catch (Exception e) {
                RangeSight.LOG.warn("Could not read {}, using defaults: {}", file, e.toString());
            }
        }
        save();
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("scanChunks", scanChunks);
        root.addProperty("lineWidth", lineWidth);
        JsonObject types = new JsonObject();
        for (RangeType type : RangeType.values()) {
            JsonObject t = new JsonObject();
            t.addProperty("enabled", enabled.contains(type));
            t.addProperty("color", String.format(Locale.ROOT, "#%06X", colors.get(type) & 0xFFFFFF));
            types.add(type.id, t);
        }
        root.add("types", types);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            RangeSight.LOG.warn("Could not save {}: {}", file, e.toString());
        }
    }

    static int parseColor(String text, int fallback) {
        try {
            String hex = text.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return 0xFF000000 | (Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (Exception e) {
            return fallback;
        }
    }
}
