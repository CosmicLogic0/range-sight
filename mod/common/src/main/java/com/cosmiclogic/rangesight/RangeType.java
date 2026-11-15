package com.cosmiclogic.rangesight;

import org.lwjgl.glfw.GLFW;

/** Every range Range Sight can show. Each has its own default color and toggle key. */
public enum RangeType {
    BEACON("beacon", 0xFF55FFFF, GLFW.GLFW_KEY_KP_1),
    CONDUIT("conduit", 0xFF3C8CFF, GLFW.GLFW_KEY_KP_2),
    SPAWNER("spawner", 0xFFFF5050, GLFW.GLFW_KEY_KP_3),
    RAID("raid", 0xFFFF9933, GLFW.GLFW_KEY_KP_4),
    SCULK("sculk", 0xFF1ED9C8, GLFW.GLFW_KEY_KP_5),
    SPAWN_CHUNKS("spawn_chunks", 0xFFFFE14D, GLFW.GLFW_KEY_KP_6);

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
    public static final int KEY_ALL_DEFAULT = GLFW.GLFW_KEY_KP_0;
}
