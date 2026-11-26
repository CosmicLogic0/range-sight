package com.cosmiclogic.rangesight;

import java.util.ArrayList;
import java.util.List;

/**
 * Every range Range Sight can show. Each has its own default color and toggle key.
 * Default keys are numpad 1-7 (numpad 0 = all), written as GLFW key numbers (321-327, 320) so this file doesn't need
 * the GLFW library on the compile path (26.3 builds don't have it).
 */
public enum RangeType {
    BEACON("beacon", 0xFF55FFFF, 321, null, null),
    CONDUIT("conduit", 0xFF3C8CFF, 322, new int[]{1, 13, 0}, null),
    SPAWNER("spawner", 0xFFFF5050, 323, null, null),
    RAID("raid", 0xFFFF9933, 324, new int[]{1, 14, 0}, null),
    SCULK("sculk", 0xFF1ED9C8, 325, new int[]{1, 17, 0}, null),
    /** Minecraft 1.21.9 removed spawn chunks. */
    SPAWN_CHUNKS("spawn_chunks", 0xFFFFE14D, 326, null, new int[]{1, 21, 9}),
    /** Trial spawners and vaults (Minecraft 1.21+). */
    TRIAL("trial", 0xFFB06CFF, 327, new int[]{1, 21, 0}, null);

    public final String id;
    public final int defaultColor;
    public final int defaultKey;
    /** First Minecraft version that has this range, or null for every version. */
    private final int[] since;
    /** First Minecraft version that no longer has it, or null. */
    private final int[] until;

    RangeType(String id, int defaultColor, int defaultKey, int[] since, int[] until) {
        this.id = id;
        this.defaultColor = defaultColor;
        this.defaultKey = defaultKey;
        this.since = since;
        this.until = until;
    }

    public String keyName() {
        return "key." + RangeSight.MOD_ID + "." + id;
    }

    public String labelKey() {
        return RangeSight.MOD_ID + ".type." + id;
    }

    /** True if this Minecraft version (e.g. {1, 20, 1} or {26, 3, 0}) has this kind of block. */
    public boolean availableIn(int[] mc) {
        return (since == null || RangeMath.compare(mc, since) >= 0) && (until == null || RangeMath.compare(mc, until) < 0);
    }

    /** The range types this Minecraft version has: only these get a key and a settings row. */
    public static List<RangeType> availableTypes(int[] mc) {
        List<RangeType> out = new ArrayList<>();
        for (RangeType type : values()) {
            if (type.availableIn(mc)) {
                out.add(type);
            }
        }
        return out;
    }

    public static RangeType byId(String id) {
        for (RangeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static final String KEY_CATEGORY = "key.categories." + RangeSight.MOD_ID;
    public static final String KEY_ALL = "key." + RangeSight.MOD_ID + ".all";
    public static final int KEY_ALL_DEFAULT = 320; // numpad 0
    public static final String KEY_SETTINGS = "key." + RangeSight.MOD_ID + ".settings";
    public static final int KEY_SETTINGS_DEFAULT = 330; // numpad . (decimal)
    /** Show or hide the HUD (1.2.0). */
    public static final String KEY_HUD = "key." + RangeSight.MOD_ID + ".hud";
    public static final int KEY_HUD_DEFAULT = 329; // numpad 9
}
