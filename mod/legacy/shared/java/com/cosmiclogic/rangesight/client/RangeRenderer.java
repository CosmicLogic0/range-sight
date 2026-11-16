package com.cosmiclogic.rangesight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Draws all outlines in one batch per frame (older versions; the vertex call differs, see {@link Mc#line}). */
public final class RangeRenderer {

    private RangeRenderer() {
    }

    public static void render(PoseStack poseStack, Vec3 camera, List<RangeShape> shapes, float lineWidth, double playerY) {
        if (shapes.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double maxDistance = (Mc.renderDistance(mc) + 2) * 16.0;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Mc.lineWidth(lineWidth);
        VertexConsumer lines = buffers.getBuffer(Mc.lineType());
        PoseStack.Pose pose = poseStack.last();
        double ringY = playerY + 0.05;
        for (RangeShape shape : shapes) {
            double reach = shape.kind == RangeShape.Kind.SPHERE ? shape.d : 0;
            if (shape.kind != RangeShape.Kind.COLUMN
                    && shape.distanceSq(camera.x, camera.y, camera.z) > (maxDistance + reach) * (maxDistance + reach)) {
                continue;
            }
            emit(lines, pose, shape.segments, camera, shape.color, lineWidth);
            emit(lines, pose, shape.playerRing(ringY), camera, shape.color, lineWidth);
        }
        buffers.endBatch(Mc.lineType());
        Mc.lineWidth(1.0f);
    }

    private static void emit(VertexConsumer lines, PoseStack.Pose pose, double[] seg, Vec3 cam, int argb, float width) {
        for (int i = 0; i + 5 < seg.length; i += 6) {
            float x1 = (float) (seg[i] - cam.x);
            float y1 = (float) (seg[i + 1] - cam.y);
            float z1 = (float) (seg[i + 2] - cam.z);
            float x2 = (float) (seg[i + 3] - cam.x);
            float y2 = (float) (seg[i + 4] - cam.y);
            float z2 = (float) (seg[i + 5] - cam.z);
            float nx = x2 - x1;
            float ny = y2 - y1;
            float nz = z2 - z1;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0e-4f) {
                continue;
            }
            nx /= len;
            ny /= len;
            nz /= len;
            Mc.line(lines, pose, x1, y1, z1, nx, ny, nz, argb, width);
            Mc.line(lines, pose, x2, y2, z2, nx, ny, nz, argb, width);
        }
    }
}
