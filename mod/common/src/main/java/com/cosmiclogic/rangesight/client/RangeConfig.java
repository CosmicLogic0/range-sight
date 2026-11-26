package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * config/rangesight.json. Every setting is listed once in {@link #SETTINGS} (key, kind, limits), and loading, saving,
 * checking, the settings screen, "Reset to defaults" and the automatic tests all go through that list.
 *
 * <p>Rules: missing keys get their defaults (so new options appear by themselves and old files keep working); a bad
 * value is reported (log + one chat note) and the default is used for that one setting; a file that isn't JSON at all
 * is kept as {@code rangesight.json.broken-<date>} and every setting starts from its default.
 */
public final class RangeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Kinds of settings, for checking values and for the settings screen. */
    public enum Kind { BOOL, INT, FLOAT, CHOICE, COLOR, COLOR_AUTO }

    /** One setting: its JSON key (dots = nested objects), the Java field behind it, and its limits. */
    public static final class Setting {
        public final String key;
        public final Kind kind;
        public final double min;
        public final double max;
        public final double step;
        public final String[] choices;
        final String field;
        /** Screen page the setting is shown on. */
        public final String page;

        Setting(String page, String key, String field, Kind kind, double min, double max, double step, String... choices) {
            this.page = page;
            this.key = key;
            this.field = field;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.step = step;
            this.choices = choices;
        }

        /** Language key of the setting's name; its tooltip is the same key + ".tip". */
        public String labelKey() {
            return RangeSight.MOD_ID + ".setting." + key;
        }
    }

    public static final String PAGE_LOOK = "look";
    public static final String PAGE_HUD = "hud";
    public static final String PAGE_BEHAVIOR = "behavior";
    public static final String PAGE_COLORS = "colors";

    /** "Auto" for an optional color (follow the range's own color). */
    public static final int AUTO = 0;

    public static final String[] STYLES = {"lines", "particles", "both"};
    public static final String[] PARTICLE_TYPES = {"dust", "end_rod", "flame", "happy_villager", "crit"};
    public static final String[] CORNERS = {"top_left", "top_right", "bottom_left", "bottom_right"};
    public static final String[] PRESETS = {"default", "colorblind", "high_contrast", "custom"};

    /** Every setting except the per-range on/off and colors (those live under "types"). */
    public static final List<Setting> SETTINGS = Collections.unmodifiableList(java.util.Arrays.asList(
            // Look
            new Setting(PAGE_LOOK, "look.style", "style", Kind.CHOICE, 0, 0, 0, STYLES),
            new Setting(PAGE_LOOK, "lineWidth", "lineWidth", Kind.FLOAT, 1, 10, 1),
            new Setting(PAGE_LOOK, "look.lineOpacity", "lineOpacity", Kind.INT, 10, 100, 10),
            new Setting(PAGE_LOOK, "look.playerRing", "playerRing", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_LOOK, "particles.type", "particleType", Kind.CHOICE, 0, 0, 0, PARTICLE_TYPES),
            new Setting(PAGE_LOOK, "particles.size", "particleSize", Kind.FLOAT, 0.25, 4, 0.25),
            new Setting(PAGE_LOOK, "particles.spacing", "particleSpacing", Kind.FLOAT, 0.5, 8, 0.5),
            new Setting(PAGE_LOOK, "particles.lifetime", "particleLifetime", Kind.INT, 5, 200, 5),
            new Setting(PAGE_LOOK, "particles.max", "particleMax", Kind.INT, 100, 5000, 100),
            // Colors that aren't a range's own color
            new Setting(PAGE_COLORS, "colors.preset", "colorPreset", Kind.CHOICE, 0, 0, 0, PRESETS),
            new Setting(PAGE_COLORS, "colors.sculkDarkness", "darknessColor", Kind.COLOR_AUTO, 0, 0, 0),
            new Setting(PAGE_COLORS, "colors.spawnerBox", "spawnBoxColor", Kind.COLOR_AUTO, 0, 0, 0),
            new Setting(PAGE_COLORS, "colors.messages", "messageColor", Kind.COLOR, 0, 0, 0),
            new Setting(PAGE_COLORS, "colors.messageOn", "messageOnColor", Kind.COLOR, 0, 0, 0),
            new Setting(PAGE_COLORS, "colors.messageOff", "messageOffColor", Kind.COLOR, 0, 0, 0),
            new Setting(PAGE_COLORS, "hud.textColor", "hudTextColor", Kind.COLOR, 0, 0, 0),
            new Setting(PAGE_COLORS, "hud.backgroundColor", "hudBackgroundColor", Kind.COLOR, 0, 0, 0),
            // HUD
            new Setting(PAGE_HUD, "hud.enabled", "hudEnabled", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.corner", "hudCorner", Kind.CHOICE, 0, 0, 0, CORNERS),
            new Setting(PAGE_HUD, "hud.offsetX", "hudOffsetX", Kind.INT, 0, 1000, 4),
            new Setting(PAGE_HUD, "hud.offsetY", "hudOffsetY", Kind.INT, 0, 1000, 4),
            new Setting(PAGE_HUD, "hud.scale", "hudScale", Kind.INT, 50, 300, 25),
            new Setting(PAGE_HUD, "hud.opacity", "hudOpacity", Kind.INT, 10, 100, 10),
            new Setting(PAGE_HUD, "hud.background", "hudBackground", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.showTitle", "hudTitle", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.showCounts", "hudCounts", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.showNearest", "hudNearest", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.showOff", "hudOffTypes", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_HUD, "hud.compact", "hudCompact", Kind.BOOL, 0, 0, 0),
            // Behavior
            new Setting(PAGE_BEHAVIOR, "scanChunks", "scanChunks", Kind.INT, 1, 16, 1),
            new Setting(PAGE_BEHAVIOR, "behavior.updateTicks", "updateTicks", Kind.INT, 5, 200, 5),
            new Setting(PAGE_BEHAVIOR, "behavior.maxOutlines", "maxOutlines", Kind.INT, 4, 256, 4),
            new Setting(PAGE_BEHAVIOR, "behavior.messages", "messages", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_BEHAVIOR, "behavior.firstJoinTip", "tip", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_BEHAVIOR, "sound.enabled", "sounds", Kind.BOOL, 0, 0, 0),
            new Setting(PAGE_BEHAVIOR, "sound.volume", "soundVolume", Kind.INT, 0, 100, 10)));

    // ---- Look (defaults = how Range Sight looked before these settings existed)
    public String style = "lines";
    /** Line width in pixels. */
    public float lineWidth = 2.0f;
    public int lineOpacity = 100;
    /** The extra ring at the player's height around spheres and columns. */
    public boolean playerRing = true;
    public String particleType = "dust";
    public float particleSize = 1.0f;
    public float particleSpacing = 1.5f;
    /** How long each particle stays, in ticks (a new set is drawn when they fade). */
    public int particleLifetime = 20;
    /** Most particles per set (protects slow computers). */
    public int particleMax = 1500;
    // ---- Colors
    public String colorPreset = "default";
    public int darknessColor = AUTO;
    public int spawnBoxColor = AUTO;
    /** The "Beacon range: ON" message above the hotbar: name, ON and OFF colors. */
    public int messageColor = 0xFF55FFFF;
    public int messageOnColor = 0xFF55FF55;
    public int messageOffColor = 0xFFFF5555;
    public int hudTextColor = 0xFFFFFFFF;
    public int hudBackgroundColor = 0xFF000000;
    // ---- HUD (off by default: Range Sight had no HUD before)
    public boolean hudEnabled = false;
    public String hudCorner = "top_left";
    public int hudOffsetX = 4;
    public int hudOffsetY = 4;
    public int hudScale = 100;
    public int hudOpacity = 100;
    public boolean hudBackground = true;
    public boolean hudTitle = true;
    public boolean hudCounts = true;
    public boolean hudNearest = true;
    public boolean hudOffTypes = false;
    public boolean hudCompact = false;
    // ---- Behavior
    /** How far to look for range blocks, in chunks. */
    public int scanChunks = 6;
    /** How often to look for range blocks again, in ticks. */
    public int updateTicks = 20;
    /** Most outlines drawn at once (the nearest ones). */
    public int maxOutlines = 48;
    /** Message above the hotbar when a range is switched. */
    public boolean messages = true;
    /** The one-time "how to use" chat tip. */
    public boolean tip = true;
    public boolean sounds = false;
    public int soundVolume = 100;

    /** Automatic test only (CI): config/rangesight-selftest.txt exists (or old "selfTest": true). Never saved. */
    public boolean selfTest;
    /** The one-time "how to use" chat message has been shown. */
    public boolean hintShown;
    /** Set when the file had a mistake and was kept as a .broken copy (shown once in chat). */
    public String brokenFileNote;
    /** Settings that had a bad value on the last load (shown once in chat, then cleared). */
    public final List<String> badValues = new ArrayList<>();

    private final Path file;
    private final Set<RangeType> enabled = EnumSet.noneOf(RangeType.class);
    private final Map<RangeType, Integer> colors = new EnumMap<>(RangeType.class);
    private static Map<String, String> defaults;

    public RangeConfig(Path configDir) {
        this.file = configDir.resolve(RangeSight.MOD_ID + ".json");
        for (RangeType type : RangeType.values()) {
            colors.put(type, type.defaultColor);
        }
        if (defaults == null) {
            Map<String, String> d = new LinkedHashMap<>();
            for (Setting s : SETTINGS) {
                d.put(s.key, get(s));
            }
            defaults = d;
        }
    }

    // ------------------------------------------------------------------ ranges on/off and colors

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

    /** Sets a range's color from text like {@code #FF8800}. Returns false (and changes nothing) for a bad value. */
    public boolean setColor(RangeType type, String hex) {
        Integer c = parseColorOrNull(hex);
        if (c == null) {
            return false;
        }
        colors.put(type, c);
        colorPreset = matchingPreset();
        save();
        return true;
    }

    /** The color used for a shrieker's 40-block Darkness ring. */
    public int darknessColor() {
        return darknessColor != AUTO ? darknessColor : darker(color(RangeType.SCULK));
    }

    /** The color used for a spawner's spawn box. */
    public int spawnBoxColor() {
        return spawnBoxColor != AUTO ? spawnBoxColor : color(RangeType.SPAWNER);
    }

    static int darker(int argb) {
        int r = (argb >> 16 & 0xFF) * 3 / 5;
        int g = (argb >> 8 & 0xFF) * 3 / 5;
        int b = (argb & 0xFF) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Colors the settings screen steps through: every type's default, then a few extra easy-to-see ones. */
    static final int[] PALETTE = {
            0xFF55FFFF, 0xFF3C8CFF, 0xFFFF5050, 0xFFFF9933, 0xFF1ED9C8, 0xFFFFE14D, 0xFFB06CFF,
            0xFFFFFFFF, 0xFFFF55FF, 0xFF55FF55, 0xFF8888FF, 0xFFAA0000};

    /** Steps the type's color to the next one in {@link #PALETTE} (a custom color goes back to the first). */
    public void nextColor(RangeType type) {
        colors.put(type, nextInPalette(colors.get(type)));
        colorPreset = matchingPreset();
        save();
    }

    static int nextInPalette(int now) {
        now |= 0xFF000000;
        int next = PALETTE[0];
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == now) {
                next = PALETTE[(i + 1) % PALETTE.length];
            }
        }
        return next;
    }

    // ------------------------------------------------------------------ color presets

    /** Range colors of a preset, in {@link RangeType} order, or null for "custom". */
    public static int[] presetColors(String preset) {
        switch (preset) {
            case "default": {
                int[] out = new int[RangeType.values().length];
                for (RangeType t : RangeType.values()) {
                    out[t.ordinal()] = t.defaultColor;
                }
                return out;
            }
            case "colorblind":
                // Okabe-Ito palette: told apart with every common kind of color blindness.
                return new int[]{0xFF56B4E9, 0xFF0072B2, 0xFFD55E00, 0xFFE69F00, 0xFF009E73, 0xFFF0E442, 0xFFCC79A7};
            case "high_contrast":
                return new int[]{0xFF00FFFF, 0xFFFFFFFF, 0xFFFF0000, 0xFFFF8000, 0xFF00FF00, 0xFFFFFF00, 0xFFFF00FF};
            default:
                return null;
        }
    }

    /** Switches every range to a preset's colors ("custom" keeps the current ones). */
    public void applyPreset(String preset) {
        int[] c = presetColors(preset);
        if (c != null) {
            for (RangeType t : RangeType.values()) {
                colors.put(t, c[t.ordinal()]);
            }
        }
        colorPreset = indexOf(PRESETS, preset) >= 0 ? preset : "custom";
        save();
    }

    private String matchingPreset() {
        for (String p : PRESETS) {
            int[] c = presetColors(p);
            if (c == null) {
                continue;
            }
            boolean same = true;
            for (RangeType t : RangeType.values()) {
                same &= colors.get(t) == c[t.ordinal()];
            }
            if (same) {
                return p;
            }
        }
        return "custom";
    }

    // ------------------------------------------------------------------ generic settings

    public static Setting setting(String key) {
        for (Setting s : SETTINGS) {
            if (s.key.equals(key)) {
                return s;
            }
        }
        throw new IllegalArgumentException("no setting " + key);
    }

    /** The default value of a setting, as text. */
    public static String defaultValue(Setting s) {
        return defaults.get(s.key);
    }

    /** The current value of a setting, as text (colors as #RRGGBB or "auto"). */
    public String get(Setting s) {
        try {
            Field f = RangeConfig.class.getField(s.field);
            Object v = f.get(this);
            switch (s.kind) {
                case COLOR:
                    return hex((Integer) v);
                case COLOR_AUTO:
                    return (Integer) v == AUTO ? "auto" : hex((Integer) v);
                case FLOAT:
                    return trim((Float) v);
                default:
                    return String.valueOf(v);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sets a setting from text. A bad or out-of-range value changes nothing and returns false (callers warn and keep
     * the default). Doesn't save; {@link #save()} does.
     */
    public boolean set(Setting s, String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        Object value;
        try {
            switch (s.kind) {
                case BOOL:
                    if (!t.equalsIgnoreCase("true") && !t.equalsIgnoreCase("false")) {
                        return false;
                    }
                    value = Boolean.parseBoolean(t);
                    break;
                case INT: {
                    double d = Double.parseDouble(t);
                    if (d != Math.rint(d) || d < s.min || d > s.max) {
                        return false;
                    }
                    value = (int) d;
                    break;
                }
                case FLOAT: {
                    float f = Float.parseFloat(t);
                    if (Float.isNaN(f) || f < s.min || f > s.max) {
                        return false;
                    }
                    value = f;
                    break;
                }
                case CHOICE: {
                    int i = indexOf(s.choices, t.toLowerCase(Locale.ROOT));
                    if (i < 0) {
                        return false;
                    }
                    value = s.choices[i];
                    break;
                }
                case COLOR_AUTO:
                    if (t.equalsIgnoreCase("auto")) {
                        value = AUTO;
                        break;
                    }
                    // fall through
                case COLOR: {
                    Integer c = parseColorOrNull(t);
                    if (c == null) {
                        return false;
                    }
                    value = c;
                    break;
                }
                default:
                    return false;
            }
            RangeConfig.class.getField(s.field).set(this, value);
            if (s.key.equals("colors.preset")) {
                int[] c = presetColors((String) value);
                if (c != null) {
                    for (RangeType type : RangeType.values()) {
                        colors.put(type, c[type.ordinal()]);
                    }
                }
            }
            return true;
        } catch (NumberFormatException | ReflectiveOperationException e) {
            return false;
        }
    }

    /** One step up (+1) or down (-1): numbers by their step, choices and colors to the next one, on/off flips. */
    public void step(Setting s, int direction) {
        String now = get(s);
        switch (s.kind) {
            case BOOL:
                set(s, String.valueOf(!Boolean.parseBoolean(now)));
                break;
            case CHOICE: {
                int i = indexOf(s.choices, now);
                set(s, s.choices[Math.floorMod(i + direction, s.choices.length)]);
                break;
            }
            case COLOR:
            case COLOR_AUTO: {
                // "auto" (optional colors only), then every palette color, then back
                List<String> options = new ArrayList<>();
                if (s.kind == Kind.COLOR_AUTO) {
                    options.add("auto");
                }
                for (int c : PALETTE) {
                    options.add(hex(c));
                }
                int i = options.indexOf(now);
                int next = i < 0 ? (direction > 0 ? 0 : -1) : i + direction;
                set(s, options.get(Math.floorMod(next, options.size())));
                break;
            }
            default: {
                double v = Double.parseDouble(now) + direction * s.step;
                // wrap around at the ends, so one button can reach every value
                if (v > s.max + 1e-9) {
                    v = s.min;
                } else if (v < s.min - 1e-9) {
                    v = s.max;
                }
                v = Math.round(v / s.step) * s.step;
                v = Math.max(s.min, Math.min(s.max, v));
                set(s, s.kind == Kind.INT ? String.valueOf((int) Math.round(v)) : trim((float) v));
            }
        }
        save();
    }

    /** Resets every setting and color to its default. Which ranges are switched on stays as it is. */
    public void resetToDefaults() {
        for (Setting s : SETTINGS) {
            set(s, defaults.get(s.key));
        }
        for (RangeType type : RangeType.values()) {
            colors.put(type, type.defaultColor);
        }
        save();
    }

    /** True if every setting and color has its default value. */
    public boolean isAllDefault() {
        for (Setting s : SETTINGS) {
            if (!get(s).equals(defaults.get(s.key))) {
                return false;
            }
        }
        for (RangeType type : RangeType.values()) {
            if (colors.get(type) != type.defaultColor) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ old stepper helpers (kept for 1.1.0 callers)

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

    // ------------------------------------------------------------------ file

    @SuppressWarnings("deprecation") // new JsonParser().parse(...) also works with the old Gson in 1.16.5
    public void load() {
        selfTest = Files.exists(file.resolveSibling(RangeSight.MOD_ID + "-selftest.txt"));
        badValues.clear();
        if (Files.exists(file)) {
            JsonObject root;
            try {
                root = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                // Never throw the player's file away: keep it next to the new one so they can fix it.
                keepBrokenFile(e);
                save();
                return;
            }
            if (root.has("selfTest") && isTrue(root.get("selfTest"))) {
                selfTest = true;
            }
            if (root.has("hintShown")) {
                hintShown = isTrue(root.get("hintShown"));
            }
            for (Setting s : SETTINGS) {
                JsonElement e = find(root, s.key);
                if (e == null) {
                    continue;
                }
                String text = e.isJsonPrimitive() ? e.getAsString() : e.toString();
                if (!set(s, text)) {
                    bad(s.key, text, defaults.get(s.key));
                }
            }
            JsonElement typesElement = root.get("types");
            JsonObject types = typesElement != null && typesElement.isJsonObject() ? typesElement.getAsJsonObject() : new JsonObject();
            for (RangeType type : RangeType.values()) {
                JsonElement te = types.get(type.id);
                if (te == null || !te.isJsonObject()) {
                    continue;
                }
                JsonObject t = te.getAsJsonObject();
                if (t.has("enabled")) {
                    JsonElement en = t.get("enabled");
                    if (en.isJsonPrimitive() && (en.getAsString().equalsIgnoreCase("true") || en.getAsString().equalsIgnoreCase("false"))) {
                        if (en.getAsString().equalsIgnoreCase("true")) {
                            enabled.add(type);
                        }
                    } else {
                        bad("types." + type.id + ".enabled", en.toString(), "false");
                    }
                }
                if (t.has("color")) {
                    JsonElement c = t.get("color");
                    Integer parsed = c.isJsonPrimitive() ? parseColorOrNull(c.getAsString()) : null;
                    if (parsed != null) {
                        colors.put(type, parsed);
                    } else {
                        bad("types." + type.id + ".color", c.toString(), hex(type.defaultColor));
                    }
                }
            }
            // A file from before presets: name the preset its colors match (or "custom").
            if (find(root, "colors.preset") == null) {
                colorPreset = matchingPreset();
            }
        }
        save();
    }

    private void bad(String key, String value, String def) {
        badValues.add(key);
        RangeSight.LOG.warn("Range Sight: config/{} has a bad value for \"{}\": {}. Using the default ({}) for that setting.",
                file.getFileName(), key, value, def);
    }

    private static boolean isTrue(JsonElement e) {
        return e.isJsonPrimitive() && e.getAsString().equalsIgnoreCase("true");
    }

    private static JsonElement find(JsonObject root, String key) {
        String[] parts = key.split("\\.");
        JsonElement at = root;
        for (String part : parts) {
            if (at == null || !at.isJsonObject()) {
                return null;
            }
            at = at.getAsJsonObject().get(part);
        }
        return at;
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
        root.addProperty("hintShown", hintShown);
        for (Setting s : SETTINGS) {
            String v = get(s);
            JsonPrimitive p;
            switch (s.kind) {
                case BOOL:
                    p = new JsonPrimitive(Boolean.parseBoolean(v));
                    break;
                case INT:
                    p = new JsonPrimitive(Integer.parseInt(v));
                    break;
                case FLOAT:
                    p = new JsonPrimitive(Float.parseFloat(v));
                    break;
                default:
                    p = new JsonPrimitive(v);
            }
            put(root, s.key, p);
        }
        JsonObject types = new JsonObject();
        for (RangeType type : RangeType.values()) {
            JsonObject t = new JsonObject();
            t.addProperty("enabled", enabled.contains(type));
            t.addProperty("color", hex(colors.get(type)));
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

    private static void put(JsonObject root, String key, JsonElement value) {
        String[] parts = key.split("\\.");
        JsonObject at = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = at.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                next = new JsonObject();
                at.add(parts[i], next);
            }
            at = next.getAsJsonObject();
        }
        at.add(parts[parts.length - 1], value);
    }

    // ------------------------------------------------------------------ helpers

    public static String hex(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    static int parseColor(String text, int fallback) {
        Integer c = parseColorOrNull(text);
        return c == null ? fallback : c;
    }

    /** {@code #RRGGBB} or {@code RRGGBB} (also 3-digit {@code #RGB}); null if it isn't a color. */
    public static Integer parseColorOrNull(String text) {
        if (text == null) {
            return null;
        }
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.length() != 6) {
            return null;
        }
        for (int i = 0; i < 6; i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return null;
            }
        }
        return 0xFF000000 | Integer.parseInt(hex, 16);
    }

    static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private static String trim(float f) {
        String s = String.valueOf(f);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
