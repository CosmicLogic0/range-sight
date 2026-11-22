package com.cosmiclogic.rangesight.paper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibVersionTest {

    @Test
    void needsAtLeastTheRequiredVersion() {
        assertTrue(LibVersion.atLeast("0.2.0", "0.2.0"));
        assertTrue(LibVersion.atLeast("0.2.1", "0.2.0"));
        assertTrue(LibVersion.atLeast("0.10.0", "0.2.0"));
        assertTrue(LibVersion.atLeast("1.0", "0.2.0"));
        assertTrue(LibVersion.atLeast("0.2.0-SNAPSHOT", "0.2.0"));
        assertFalse(LibVersion.atLeast("0.1.0", "0.2.0"));
        assertFalse(LibVersion.atLeast("0.1.9", "0.2.0"));
        assertFalse(LibVersion.atLeast(null, "0.2.0"));
        assertFalse(LibVersion.atLeast("", "0.2.0"));
    }

    @Test
    void parsesOddVersionStrings() {
        assertArrayEquals(new int[]{0, 2, 0}, LibVersion.parse("v0.2"));
        assertArrayEquals(new int[]{1, 2, 3}, LibVersion.parse("1.2.3.4"));
        assertArrayEquals(new int[]{0, 0, 0}, LibVersion.parse("banana"));
    }
}
