package com.cosmiclogic.rangesight.paper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Checks the range rules against the vanilla numbers. */
class RangeMathTest {

    @Test
    void beaconRanges() {
        assertEquals(0, RangeMath.beaconRange(0));
        assertEquals(20, RangeMath.beaconRange(1));
        assertEquals(30, RangeMath.beaconRange(2));
        assertEquals(40, RangeMath.beaconRange(3));
        assertEquals(50, RangeMath.beaconRange(4));
    }

    @Test
    void conduitRanges() {
        assertEquals(0, RangeMath.conduitRange(15));
        assertEquals(32, RangeMath.conduitRange(16));
        assertEquals(32, RangeMath.conduitRange(20));
        assertEquals(48, RangeMath.conduitRange(21));
        assertEquals(64, RangeMath.conduitRange(28));
        assertEquals(80, RangeMath.conduitRange(35));
        assertEquals(96, RangeMath.conduitRange(42));
    }

    @Test
    void conduitHas42FramePositions() {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (RangeMath.isConduitFramePosition(dx, dy, dz)) {
                        count++;
                    }
                }
            }
        }
        assertEquals(42, count);
    }

    @Test
    void sculk() {
        assertEquals(8, RangeMath.sculkSensorRange(false));
        assertEquals(16, RangeMath.sculkSensorRange(true));
    }

    @Test
    void slice() {
        assertEquals(10.0, RangeMath.sliceRadius(10, 0), 1e-9);
        assertEquals(8.0, RangeMath.sliceRadius(10, 6), 1e-9);
        assertEquals(0.0, RangeMath.sliceRadius(10, 11), 1e-9);
    }

    @Test
    void spawnChunks() {
        // 1.14 - 1.20.4: 19x19 around the spawn chunk.
        assertArrayEquals(new int[]{-9, -9, 9, 9}, RangeMath.spawnChunks(new int[]{1, 20, 1}, 0, 0, 2));
        assertArrayEquals(new int[]{-9, -9, 9, 9}, RangeMath.spawnChunks(new int[]{1, 18, 2}, 15, 15, 2));
        // 1.20.5 - 1.21.8: game rule radius 2 = 3x3.
        assertArrayEquals(new int[]{-1, -1, 1, 1}, RangeMath.spawnChunks(new int[]{1, 21, 1}, 0, 0, 2));
        assertArrayEquals(new int[]{-9, -9, 9, 9}, RangeMath.spawnChunks(new int[]{1, 21, 1}, 0, 0, 10));
        assertNull(RangeMath.spawnChunks(new int[]{1, 21, 1}, 0, 0, 0));
        // 1.21.9+ and 26.x: none.
        assertNull(RangeMath.spawnChunks(new int[]{1, 21, 11}, 0, 0, 2));
        assertNull(RangeMath.spawnChunks(new int[]{26, 1, 2}, 0, 0, 2));
        // 1.12.2: chunk middles within 128 blocks of spawn (0,0): chunks -8..7.
        assertArrayEquals(new int[]{-8, -8, 7, 7}, RangeMath.spawnChunks(new int[]{1, 12, 2}, 0, 0, 2));
    }
}
