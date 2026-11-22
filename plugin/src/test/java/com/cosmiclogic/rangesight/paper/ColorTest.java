package com.cosmiclogic.rangesight.paper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** config.yml colors: "#RRGGBB" or "RRGGBB"; anything else is rejected (and the default is used). */
class ColorTest {

    @Test
    void readsHexColors() {
        assertEquals(0x55FFFF, RangeType.parseColor("#55FFFF").asRGB());
        assertEquals(0xB06CFF, RangeType.parseColor("b06cff").asRGB());
        assertEquals(0x000000, RangeType.parseColor(" #000000 ").asRGB());
    }

    @Test
    void rejectsBrokenColors() {
        assertNull(RangeType.parseColor("red"));
        assertNull(RangeType.parseColor("#FFF"));
        assertNull(RangeType.parseColor("#GG0000"));
        assertNull(RangeType.parseColor(""));
        assertNull(RangeType.parseColor(null));
    }

    @Test
    void everyTypeHasAUniqueId() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (RangeType type : RangeType.values()) {
            assertEquals(true, ids.add(type.id), "duplicate id " + type.id);
            assertEquals(type, RangeType.byId(type.id.toUpperCase()));
        }
    }
}
