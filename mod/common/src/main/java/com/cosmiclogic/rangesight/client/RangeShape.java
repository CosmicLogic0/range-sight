package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeType;

/**
 * One outline, built once when blocks are scanned (not every frame).
 * {@link #segments} holds line segments as x1,y1,z1,x2,y2,z2 in world coordinates.
 * Columns and spheres also get one extra ring at the player's height, worked out per frame.
 */
public final class RangeShape {

    public enum Kind { BOX, COLUMN, SPHERE }

    private static final int RING_SEGMENTS = 48;

    public final RangeType type;
    public final Kind kind;
    public final double[] segments;
    public final int color;
    // Column: x1, z1, x2, z2, minY, maxY. Sphere: cx, cy, cz, radius.
    public final double a, b, c, d, minY, maxY;

    private RangeShape(RangeType type, Kind kind, double[] segments, int color,
                       double a, double b, double c, double d, double minY, double maxY) {
        this.type = type;
        this.kind = kind;
        this.segments = segments;
        this.color = color;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** A box (12 edges). */
    public static RangeShape box(RangeType type, int color, double x1, double y1, double z1, double x2, double y2, double z2) {
        Segments s = new Segments(12);
        rect(s, x1, z1, x2, z2, y1);
        rect(s, x1, z1, x2, z2, y2);
        s.add(x1, y1, z1, x1, y2, z1);
        s.add(x2, y1, z1, x2, y2, z1);
        s.add(x2, y1, z2, x2, y2, z2);
        s.add(x1, y1, z2, x1, y2, z2);
        return new RangeShape(type, Kind.BOX, s.done(), color, x1, z1, x2, z2, y1, y2);
    }

    /** A tall square area (beacon reach, spawn chunks): drawn as a box plus a ring at the player's height. */
    public static RangeShape column(RangeType type, int color, double x1, double z1, double x2, double z2, double minY, double maxY) {
        RangeShape box = box(type, color, x1, minY, z1, x2, maxY, z2);
        return new RangeShape(type, Kind.COLUMN, box.segments, color, x1, z1, x2, z2, minY, maxY);
    }

    /** A sphere: 5 rings around it, 4 rings over the top, plus a ring at the player's height. */
    public static RangeShape sphere(RangeType type, int color, double cx, double cy, double cz, double radius) {
        Segments s = new Segments(RING_SEGMENTS * 9);
        for (int i = -2; i <= 2; i++) {
            double dy = radius * i / 3.0;
            horizontalRing(s, cx, cy + dy, cz, RangeMath.sliceRadius(radius, dy));
        }
        for (int m = 0; m < 4; m++) {
            double angle = Math.PI * m / 4;
            double ux = Math.cos(angle);
            double uz = Math.sin(angle);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double t1 = 2 * Math.PI * i / RING_SEGMENTS;
                double t2 = 2 * Math.PI * (i + 1) / RING_SEGMENTS;
                s.add(cx + ux * Math.cos(t1) * radius, cy + Math.sin(t1) * radius, cz + uz * Math.cos(t1) * radius,
                        cx + ux * Math.cos(t2) * radius, cy + Math.sin(t2) * radius, cz + uz * Math.cos(t2) * radius);
            }
        }
        return new RangeShape(type, Kind.SPHERE, s.done(), color, cx, cy, cz, radius, cy - radius, cy + radius);
    }

    /** Squared distance from a point to the middle of the shape (for picking the nearest ones). */
    public double distanceSq(double x, double y, double z) {
        double mx;
        double my;
        double mz;
        if (kind == Kind.SPHERE) {
            mx = a;
            my = b;
            mz = c;
        } else {
            mx = (a + c) / 2;
            my = Math.max(minY, Math.min(maxY, y));
            mz = (b + d) / 2;
        }
        double dx = mx - x;
        double dy = my - y;
        double dz = mz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The ring at the player's height y (empty if the player is above or below the shape). */
    public double[] playerRing(double y) {
        Segments s = new Segments(RING_SEGMENTS);
        if (kind == Kind.COLUMN) {
            if (y >= minY && y <= maxY) {
                rect(s, a, b, c, d, y);
            }
        } else if (kind == Kind.SPHERE) {
            double r = RangeMath.sliceRadius(d, y - b);
            if (r > 0.5) {
                horizontalRing(s, a, y, c, r);
            }
        }
        return s.done();
    }

    static void rect(Segments s, double x1, double z1, double x2, double z2, double y) {
        s.add(x1, y, z1, x2, y, z1);
        s.add(x2, y, z1, x2, y, z2);
        s.add(x2, y, z2, x1, y, z2);
        s.add(x1, y, z2, x1, y, z1);
    }

    static void horizontalRing(Segments s, double cx, double y, double cz, double r) {
        if (r <= 0) {
            return;
        }
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double t1 = 2 * Math.PI * i / RING_SEGMENTS;
            double t2 = 2 * Math.PI * (i + 1) / RING_SEGMENTS;
            s.add(cx + Math.cos(t1) * r, y, cz + Math.sin(t1) * r, cx + Math.cos(t2) * r, y, cz + Math.sin(t2) * r);
        }
    }

    /** Small growable list of segments. */
    static final class Segments {
        private double[] data;
        private int size;

        Segments(int expected) {
            data = new double[Math.max(6, expected * 6)];
        }

        void add(double x1, double y1, double z1, double x2, double y2, double z2) {
            if (size + 6 > data.length) {
                data = java.util.Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = x1;
            data[size++] = y1;
            data[size++] = z1;
            data[size++] = x2;
            data[size++] = y2;
            data[size++] = z2;
        }

        double[] done() {
            return java.util.Arrays.copyOf(data, size);
        }
    }
}
