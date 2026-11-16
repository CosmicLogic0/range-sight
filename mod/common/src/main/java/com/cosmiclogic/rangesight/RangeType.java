package com.cosmiclogic.rangesight;

/**
 * Every range Range Sight can show. Each has its own default color and toggle key.
 * Default keys are numpad 1-6 (numpad 0 = all), written as GLFW key numbers (321-326, 320) so this file doesn't need
 * the GLFW library on the compile path (26.3 builds don't have it).
 */
public enum RangeType {
    BEACON("beacon", 0xFF55FFFF, 321),
    CONDUIT("conduit", 0xFF3C8CFF, 322),
    SPAWNER("spawner", 0xFFFF5050, 323),
    RAID("raid", 0xFFFF9933, 324),
    SCULK("sculk", 0xFF1ED9C8, 325),
    SPAWN_CHUNKS("spawn_chunks", 0xFFFFE14D, 326);

    public final String id;
    public final int defaultColor;
    public final int defaultKey;

    RangeType(String id, int defaultColor, int defaultKey) {
        this.id = id;
        this.defaultColor = defaultColor;
        this.defaultKey = defaultKey;
    }

    public String keyName() {
        return "key." + RangeSight.MOD_ID + "." + id;
    }

    public String labelKey() {
        return RangeSight.MOD_ID + ".type." + id;
    }

    public static final String KEY_CATEGORY = "key.categories." + RangeSight.MOD_ID;
    public static final String KEY_ALL = "key." + RangeSight.MOD_ID + ".all";
    public static final int KEY_ALL_DEFAULT = 320; // numpad 0
}
