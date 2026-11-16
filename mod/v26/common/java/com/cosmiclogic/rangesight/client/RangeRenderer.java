package com.cosmiclogic.rangesight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 26.x: draws the outlines as gizmos (Minecraft's own outline system). The shapes are built by
 * {@link RangeScanner} once a second, so a frame only hands ready-made line points to Minecraft.
 * Each frame is capped, so huge bases stay smooth.
 */
public final class RangeRenderer {

    /** Most line pieces drawn per frame. */
    private static final int MAX_SEGMENTS_PER_FRAME = 12000;

    private RangeRenderer() {
    }

    public static void render(List<RangeShape> shapes, float lineWidth, double px, double py, double pz) {
        Minecraft mc = Minecraft.getInstance();
        double maxDistance = (mc.options.getEffectiveRenderDistance() + 2) * 16.0;
        int budget = MAX_SEGMENTS_PER_FRAME;
        double ringY = py + 0.05;
        for (RangeShape shape : shapes) {
            double reach = shape.kind == RangeShape.Kind.SPHERE ? shape.d : 0;
            if (shape.kind != RangeShape.Kind.COLUMN
                    && shape.distanceSq(px, py, pz) > (maxDistance + reach) * (maxDistance + reach)) {
                continue;
            }
            budget = emit(shape.segments, shape.color, lineWidth, budget);
            budget = emit(shape.playerRing(ringY), shape.color, lineWidth, budget);
            if (budget <= 0) {
                break;
            }
        }
    }

    private static int emit(double[] seg, int argb, float width, int budget) {
        for (int i = 0; i + 5 < seg.length && budget > 0; i += 6, budget--) {
            Gizmos.line(new Vec3(seg[i], seg[i + 1], seg[i + 2]), new Vec3(seg[i + 3], seg[i + 4], seg[i + 5]), argb, width);
        }
        return budget;
    }
}
