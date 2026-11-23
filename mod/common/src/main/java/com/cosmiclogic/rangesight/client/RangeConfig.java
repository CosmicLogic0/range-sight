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
    /** Automatic test only (CI): config/rangesight-selftest.txt exists (or old "selfTest": true). Never saved. */
    public boolean selfTest;
    /** The one-time "how to use" chat message has been shown. */
    public boolean hintShown;
    /** Set when the file had a mistake and was kept as a .broken copy (shown once in chat). */
    public String brokenFileNote;

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

    /** Colors the settings screen steps through: every type's default, then a few extra easy-to-see ones. */
    static final int[] PALETTE = {
            0xFF55FFFF, 0xFF3C8CFF, 0xFFFF5050, 0xFFFF9933, 0xFF1ED9C8, 0xFFFFE14D, 0xFFB06CFF,
            0xFFFFFFFF, 0xFFFF55FF, 0xFF55FF55, 0xFF8888FF, 0xFFAA0000};

    /** Steps the type's color to the next one in {@link #PALETTE} (a custom color goes back to the first). */
    public void nextColor(RangeType type) {
        int now = colors.get(type) | 0xFF000000;
        int next = PALETTE[0];
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == now) {
                next = PALETTE[(i + 1) % PALETTE.length];
            }
        }
        colors.put(type, next);
        save();
    }

    /** Line width 1 to 5, then back to 1. */
    public void nextLineWidth() {
        int w = Math.round(lineWidth);
        lineWidth = w >= 5 ? 1f : w + 1f;
        save();
    }

    /** Search distance in chunks: 2, 4, 6, 8, 12, 16, then back to 2. */
    public void nextScanChunks() {
        int[] steps = {2, 4, 6, 8, 12, 16};
        int next = steps[0];
        for (int step : steps) {
            if (step > scanChunks) {
                next = step;
                break;
            }
        }
        scanChunks = next;
        save();
    }

    @SuppressWarnings("deprecation") // new JsonParser().parse(...) also works with the old Gson in 1.16.5
    public void load() {
        selfTest = Files.exists(file.resolveSibling(RangeSight.MOD_ID + "-selftest.txt"));
        if (Files.exists(file)) {
            try {
                JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("scanChunks")) {
                    scanChunks = Math.max(1, Math.min(16, root.get("scanChunks").getAsInt()));
                }
                if (root.has("selfTest") && root.get("selfTest").getAsBoolean()) {
                    selfTest = true;
                }
                if (root.has("hintShown")) {
                    hintShown = root.get("hintShown").getAsBoolean();
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
                // Never throw the player's file away: keep it next to the new one so they can fix it.
                keepBrokenFile(e);
            }
        }
        save();
    }

    private void keepBrokenFile(Exception problem) {
        enabled.clear();
        for (RangeType type : RangeType.values()) {
            colors.put(type, type.defaultColor);
        }
        Path backup = file.resolveSibling(file.getFileName() + ".broken-"
                + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new java.util.Date()));
        try {
            Files.move(file, backup);
            brokenFileNote = backup.getFileName().toString();
            RangeSight.LOG.warn("Range Sight: config/{} has a mistake ({}). Using the default settings; your file was kept as config/{}",
                    file.getFileName(), problem.getMessage(), backup.getFileName());
        } catch (IOException e) {
            RangeSight.LOG.warn("Range Sight: config/{} has a mistake ({}) and could not be set aside ({}); using defaults",
                    file.getFileName(), problem.getMessage(), e.toString());
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("scanChunks", scanChunks);
        root.addProperty("lineWidth", lineWidth);
        root.addProperty("hintShown", hintShown);
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
            Files.write(file, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
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
