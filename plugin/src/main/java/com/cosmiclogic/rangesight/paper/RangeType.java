package com.cosmiclogic.rangesight.paper;

import org.bukkit.Color;

/** The range types /range can show, each with its own color. */
public enum RangeType {
    BEACON("beacon", "Beacons", Color.fromRGB(0x55FFFF)),
    CONDUIT("conduit", "Conduits", Color.fromRGB(0x3C8CFF)),
    SPAWNER("spawner", "Spawners", Color.fromRGB(0xFF5050)),
    RAID("raid", "Raids", Color.fromRGB(0xFF9933)),
    SCULK("sculk", "Sculk sensors and shriekers", Color.fromRGB(0x1ED9C8)),
    SPAWN_CHUNKS("spawnchunks", "Spawn chunks", Color.fromRGB(0xFFE14D));

    public final String id;
    public final String label;
    public final Color color;

    RangeType(String id, String label, Color color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static RangeType byId(String id) {
        for (RangeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
