package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeConfigTest {

    @TempDir
    Path dir;

    private void write(String json) throws IOException {
        Files.write(dir.resolve("rangesight.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void brokenFileIsKeptNotOverwritten() throws IOException {
        write("{\"types\":{\"beacon\":{\"enabled\":tru");
        RangeConfig config = new RangeConfig(dir);
        config.load();
        assertNotNull(config.brokenFileNote);
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("rangesight.json.broken-")));
        }
        String kept = new String(Files.readAllBytes(dir.resolve(config.brokenFileNote)), StandardCharsets.UTF_8);
        assertEquals("{\"types\":{\"beacon\":{\"enabled\":tru", kept);
        assertFalse(config.anyEnabled());
    }

    @Test
    void version010FileStillLoads() throws IOException {
        // A file written by Range Sight 0.1.0 (no trial type, no hintShown, old selfTest flag).
        write("{\"scanChunks\":4,\"lineWidth\":3.0,\"selfTest\":false,\"types\":{"
                + "\"beacon\":{\"enabled\":true,\"color\":\"#112233\"},\"spawner\":{\"enabled\":false,\"color\":\"#FF5050\"}}}");
        RangeConfig config = new RangeConfig(dir);
        config.load();
        assertEquals(4, config.scanChunks);
        assertEquals(3.0f, config.lineWidth);
        assertTrue(config.isEnabled(RangeType.BEACON));
        assertFalse(config.isEnabled(RangeType.TRIAL));
        assertEquals(0xFF112233, config.color(RangeType.BEACON));
        assertEquals(RangeType.TRIAL.defaultColor, config.color(RangeType.TRIAL));
        assertEquals(null, config.brokenFileNote);
        // and it is saved back with the new type added
        assertTrue(new String(Files.readAllBytes(dir.resolve("rangesight.json")), StandardCharsets.UTF_8).contains("\"trial\""));
    }

    @Test
    void badValuesAreClampedNotFatal() throws IOException {
        write("{\"scanChunks\":999,\"lineWidth\":-4,\"types\":{\"beacon\":{\"enabled\":true,\"color\":\"not a color\"}}}");
        RangeConfig config = new RangeConfig(dir);
        config.load();
        assertEquals(16, config.scanChunks);
        assertEquals(1f, config.lineWidth);
        assertEquals(RangeType.BEACON.defaultColor, config.color(RangeType.BEACON));
        assertTrue(config.isEnabled(RangeType.BEACON));
    }

    @Test
    void selfTestMarkerFileTurnsOnTheTest() throws IOException {
        Files.write(dir.resolve("rangesight-selftest.txt"), "on".getBytes(StandardCharsets.UTF_8));
        RangeConfig config = new RangeConfig(dir);
        config.load();
        assertTrue(config.selfTest);
        assertFalse(new String(Files.readAllBytes(dir.resolve("rangesight.json")), StandardCharsets.UTF_8).contains("selfTest"));
    }

    @Test
    void settingsScreenSteppersWrapAround() {
        RangeConfig config = new RangeConfig(dir);
        int first = config.color(RangeType.BEACON);
        for (int i = 0; i < RangeConfig.PALETTE.length; i++) {
            config.nextColor(RangeType.BEACON);
            if (i < RangeConfig.PALETTE.length - 1) {
                assertNotEquals(first, config.color(RangeType.BEACON));
            }
        }
        assertEquals(first, config.color(RangeType.BEACON));
        config.lineWidth = 5f;
        config.nextLineWidth();
        assertEquals(1f, config.lineWidth);
        config.scanChunks = 16;
        config.nextScanChunks();
        assertEquals(2, config.scanChunks);
        config.nextScanChunks();
        assertEquals(4, config.scanChunks);
    }
}
