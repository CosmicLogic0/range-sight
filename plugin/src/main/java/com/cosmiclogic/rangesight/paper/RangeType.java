package com.cosmiclogic.rangesight.paper;

import org.bukkit.Color;

/** The range types /range can show, each with its own color (changeable in config.yml). */
public enum RangeType {
    BEACON("beacon", "Beacons", 0x55FFFF, "how far a beacon's effects reach"),
    CONDUIT("conduit", "Conduits", 0x3C8CFF, "where a conduit gives Conduit Power"),
    SPAWNER("spawner", "Spawners", 0xFF5050, "how close you must be for a spawner to work, and where its mobs appear"),
    RAID("raid", "Raids", 0xFF9933, "the area around an active raid"),
    SCULK("sculk", "Sculk sensors and shriekers", 0x1ED9C8, "how far sculk blocks hear you, and a shrieker's Darkness"),
    SPAWN_CHUNKS("spawnchunks", "Spawn chunks", 0xFFE14D, "the chunks that stay loaded around world spawn (up to 1.21.8)"),
    TRIAL("trial", "Trial spawners and vaults", 0xB06CFF, "how close you must be for a trial spawner or vault to wake up (1.21+)");

    public final String id;
    public final String label;
    public final int defaultRgb;
    public final String help;
    /** Current color (from config.yml, or the default). */
    public Color color;

    RangeType(String id, String label, int defaultRgb, String help) {
        this.id = id;
        this.label = label;
        this.defaultRgb = defaultRgb;
        this.help = help;
        this.color = Color.fromRGB(defaultRgb);
    }

    public static RangeType byId(String id) {
        for (RangeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    /** "#RRGGBB" or "RRGGBB" to a color; null if it isn't one. */
    static Color parseColor(String text) {
        if (text == null) {
            return null;
        }
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return Color.fromRGB(Integer.parseInt(hex, 16));
    }
}
