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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeConfigTest {

    @TempDir
    Path dir;

    private void write(String json) throws IOException {
        Files.write(dir.resolve("rangesight.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private String file() throws IOException {
        return new String(Files.readAllBytes(dir.resolve("rangesight.json")), StandardCharsets.UTF_8);
    }

    private RangeConfig load() {
        RangeConfig config = new RangeConfig(dir);
        config.load();
        return config;
    }

    @Test
    void brokenFileIsKeptNotOverwritten() throws IOException {
        write("{\"types\":{\"beacon\":{\"enabled\":tru");
        RangeConfig config = load();
        assertNotNull(config.brokenFileNote);
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("rangesight.json.broken-")));
        }
        String kept = new String(Files.readAllBytes(dir.resolve(config.brokenFileNote)), StandardCharsets.UTF_8);
        assertEquals("{\"types\":{\"beacon\":{\"enabled\":tru", kept);
        assertFalse(config.anyEnabled());
        assertTrue(config.isAllDefault());
    }

    @Test
    void version010FileStillLoads() throws IOException {
        // A file written by Range Sight 0.1.0 (no trial type, no hintShown, old selfTest flag).
        write("{\"scanChunks\":4,\"lineWidth\":3.0,\"selfTest\":false,\"types\":{"
                + "\"beacon\":{\"enabled\":true,\"color\":\"#112233\"},\"spawner\":{\"enabled\":false,\"color\":\"#FF5050\"}}}");
        RangeConfig config = load();
        assertEquals(4, config.scanChunks);
        assertEquals(3.0f, config.lineWidth);
        assertTrue(config.isEnabled(RangeType.BEACON));
        assertFalse(config.isEnabled(RangeType.TRIAL));
        assertEquals(0xFF112233, config.color(RangeType.BEACON));
        assertEquals(RangeType.TRIAL.defaultColor, config.color(RangeType.TRIAL));
        assertEquals("custom", config.colorPreset);
        assertNull(config.brokenFileNote);
        assertTrue(config.badValues.isEmpty());
        // and it is saved back with the new type and the new settings added
        assertTrue(file().contains("\"trial\""));
        assertTrue(file().contains("\"hud\""));
    }

    @Test
    void version110FileKeepsEverythingAndLooksTheSame() throws IOException {
        write("{\"scanChunks\":6,\"lineWidth\":2.0,\"hintShown\":true,\"types\":{"
                + "\"beacon\":{\"enabled\":true,\"color\":\"#55FFFF\"},\"trial\":{\"enabled\":true,\"color\":\"#B06CFF\"}}}");
        RangeConfig config = load();
        assertTrue(config.hintShown);
        assertTrue(config.isEnabled(RangeType.TRIAL));
        assertEquals("default", config.colorPreset);
        assertTrue(config.isAllDefault(), "a 1.1.0 file must give exactly the 1.1.0 look and behavior");
        assertEquals("lines", config.style);
        assertFalse(config.hudEnabled);
        assertFalse(config.sounds);
    }

    @Test
    void everySettingFallsBackToItsDefaultOnABadValue() throws IOException {
        for (RangeConfig.Setting s : RangeConfig.SETTINGS) {
            String badJson;
            switch (s.kind) {
                case BOOL:
                    badJson = "\"maybe\"";
                    break;
                case INT:
                case FLOAT:
                    badJson = String.valueOf(s.max + 1000);
                    break;
                default:
                    badJson = "\"not-a-value\"";
            }
            write(nested(s.key, badJson));
            RangeConfig config = load();
            assertEquals(RangeConfig.defaultValue(s), config.get(s), s.key);
            assertTrue(config.badValues.contains(s.key), s.key + " should be reported");
            assertNull(config.brokenFileNote, s.key + ": one bad value must not throw the file away");
        }
    }

    @Test
    void everySettingKeepsAGoodValueAndResetBringsBackTheDefault() {
        RangeConfig config = new RangeConfig(dir);
        for (RangeConfig.Setting s : RangeConfig.SETTINGS) {
            String before = config.get(s);
            config.step(s, 1);
            assertNotEquals(before, config.get(s), s.key + " should change");
            String changed = config.get(s);
            RangeConfig again = load();
            assertEquals(changed, again.get(s), s.key + " should be saved");
            config = again;
        }
        assertFalse(config.isAllDefault());
        config.setEnabled(RangeType.BEACON, true);
        config.resetToDefaults();
        assertTrue(config.isAllDefault());
        assertTrue(config.isEnabled(RangeType.BEACON), "reset keeps which ranges are on");
        assertTrue(load().isAllDefault());
    }

    @Test
    void textValuesAreChecked() {
        RangeConfig config = new RangeConfig(dir);
        RangeConfig.Setting scale = RangeConfig.setting("hud.scale");
        assertTrue(config.set(scale, "150"));
        assertFalse(config.set(scale, "150.5"));
        assertFalse(config.set(scale, "20"));
        assertFalse(config.set(scale, "abc"));
        assertEquals("150", config.get(scale));
        RangeConfig.Setting dark = RangeConfig.setting("colors.sculkDarkness");
        assertTrue(config.set(dark, "#FF8800"));
        assertEquals(0xFFFF8800, config.darknessColor());
        assertTrue(config.set(dark, "auto"));
        assertEquals(RangeConfig.darker(config.color(RangeType.SCULK)), config.darknessColor());
        assertFalse(config.set(dark, "#GG0000"));
        assertTrue(config.setColor(RangeType.BEACON, "f80"));
        assertEquals(0xFFFF8800, config.color(RangeType.BEACON));
        assertFalse(config.setColor(RangeType.BEACON, "#12345"));
    }

    @Test
    void presetsIncludingColorBlind() {
        RangeConfig config = new RangeConfig(dir);
        config.applyPreset("colorblind");
        assertEquals(0xFF56B4E9, config.color(RangeType.BEACON));
        assertEquals("colorblind", config.colorPreset);
        config.setColor(RangeType.BEACON, "#000000");
        assertEquals("custom", config.colorPreset);
        config.applyPreset("default");
        assertTrue(config.isAllDefault());
        for (String p : RangeConfig.PRESETS) {
            int[] c = RangeConfig.presetColors(p);
            if (c == null) {
                continue;
            }
            for (int i = 0; i < c.length; i++) {
                for (int j = i + 1; j < c.length; j++) {
                    assertNotEquals(c[i], c[j], p + ": two ranges share a color");
                }
            }
        }
    }

    @Test
    void selfTestMarkerFileTurnsOnTheTest() throws IOException {
        Files.write(dir.resolve("rangesight-selftest.txt"), "on".getBytes(StandardCharsets.UTF_8));
        RangeConfig config = load();
        assertTrue(config.selfTest);
        assertFalse(file().contains("selfTest"));
    }

    @Test
    void oldSteppersStillWrapAround() {
        RangeConfig config = new RangeConfig(dir);
        int first = config.color(RangeType.BEACON);
        for (int i = 0; i < RangeConfig.PALETTE.length; i++) {
            config.nextColor(RangeType.BEACON);
        }
        assertEquals(first, config.color(RangeType.BEACON));
        config.lineWidth = 5f;
        config.nextLineWidth();
        assertEquals(1f, config.lineWidth);
        config.scanChunks = 16;
        config.nextScanChunks();
        assertEquals(2, config.scanChunks);
    }

    private static String nested(String key, String value) {
        String[] parts = key.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append("{\"").append(p).append("\":");
        }
        sb.append(value);
        for (int i = 0; i < parts.length; i++) {
            sb.append('}');
        }
        return sb.toString();
    }
}
