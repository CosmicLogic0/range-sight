package com.cosmiclogic.rangesight.paper;

/** Compares plugin versions like "0.2.0" or "0.10.1-SNAPSHOT" (only the numbers count). */
final class LibVersion {

    /** The oldest LogicLib Plugin that has everything Range Sight uses (LogicParticles.FAR, LogicConfigs). */
    static final String NEEDED = "0.2.0";

    private LibVersion() {
    }

    static boolean atLeast(String have, String need) {
        int[] a = parse(have);
        int[] b = parse(need);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        return true;
    }

    static int[] parse(String version) {
        int[] out = new int[3];
        if (version == null) {
            return out;
        }
        String[] parts = version.trim().split("[^0-9]+");
        int n = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (n >= 3) {
                break;
            }
            try {
                out[n++] = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                out[n++] = 0;
            }
        }
        return out;
    }
}
