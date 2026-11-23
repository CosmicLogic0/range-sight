package com.cosmiclogic.rangesight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeTypeTest {

    @Test
    void onlyRangesTheVersionHasGetKeys() {
        assertEquals(List.of(RangeType.BEACON, RangeType.SPAWNER, RangeType.SPAWN_CHUNKS),
                RangeType.availableTypes(new int[]{1, 12, 2}));
        assertFalse(RangeType.SCULK.availableIn(new int[]{1, 16, 5}));
        assertTrue(RangeType.SCULK.availableIn(new int[]{1, 18, 2}));
        assertFalse(RangeType.TRIAL.availableIn(new int[]{1, 20, 1}));
        assertTrue(RangeType.TRIAL.availableIn(new int[]{1, 21, 1}));
    }

    @Test
    void spawnChunksStopAt1219() {
        assertTrue(RangeType.SPAWN_CHUNKS.availableIn(new int[]{1, 21, 8}));
        assertFalse(RangeType.SPAWN_CHUNKS.availableIn(new int[]{1, 21, 9}));
        assertFalse(RangeType.SPAWN_CHUNKS.availableIn(new int[]{1, 21, 11}));
        assertFalse(RangeType.SPAWN_CHUNKS.availableIn(new int[]{26, 0, 0}));
        assertTrue(RangeType.TRIAL.availableIn(new int[]{26, 3, 0}));
    }

    @Test
    void everyTypeHasItsOwnKeyAndColor() {
        for (RangeType a : RangeType.values()) {
            assertTrue(a.defaultKey != RangeType.KEY_ALL_DEFAULT && a.defaultKey != RangeType.KEY_SETTINGS_DEFAULT);
            for (RangeType b : RangeType.values()) {
                if (a != b) {
                    assertTrue(a.defaultKey != b.defaultKey, a + " and " + b + " share a key");
                    assertTrue(a.defaultColor != b.defaultColor, a + " and " + b + " share a color");
                }
            }
        }
    }

    @Test
    void byIdIgnoresCase() {
        assertSame(RangeType.SPAWN_CHUNKS, RangeType.byId("Spawn_Chunks"));
        assertNull(RangeType.byId("nope"));
    }

    @Test
    void versionCompare() {
        assertTrue(RangeMath.compare(new int[]{1, 21, 11}, new int[]{1, 21, 9}) > 0);
        assertTrue(RangeMath.compare(new int[]{26, 1, 2}, new int[]{1, 21, 11}) > 0);
        assertEquals(0, RangeMath.compare(new int[]{1, 20, 1}, new int[]{1, 20, 1}));
    }
}
