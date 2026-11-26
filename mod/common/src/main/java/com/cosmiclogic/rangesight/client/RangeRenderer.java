package com.cosmiclogic.rangesight.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws all outlines in one batch per frame. The shapes are built by {@link RangeScanner}
 * once a second, so a frame only copies ready-made line points (no block lookups, no math per block).
 */
public final class RangeRenderer {

    private RangeRenderer() {
    }

    /**
     * @param poseStack the level pose stack (camera rotation applied, not the camera position)
     * @param camera    camera position in the world
     * @param playerY   the player's feet height (for the ring at your height)
     */
    public static void render(PoseStack poseStack, Vec3 camera, List<RangeShape> shapes, float lineWidth, double playerY) {
        if (shapes.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double maxDistance = (mc.options.getEffectiveRenderDistance() + 2) * 16.0;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderSystem.lineWidth(lineWidth);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        double ringY = playerY + 0.05;
        RangeConfig config = RangeClient.config();
        int alpha = config == null ? 255 : Math.round(255 * config.lineOpacity / 100f);
        boolean ring = config == null || config.playerRing;

        for (RangeShape shape : shapes) {
            double reach = shape.kind == RangeShape.Kind.SPHERE ? shape.d : 0;
            if (shape.distanceSq(camera.x, camera.y, camera.z) > (maxDistance + reach) * (maxDistance + reach) && shape.kind != RangeShape.Kind.COLUMN) {
                continue;
            }
            emit(lines, pose, shape.segments, camera, shape.color, alpha);
            if (ring) {
                emit(lines, pose, shape.playerRing(ringY), camera, shape.color, alpha);
            }
        }
        buffers.endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0f);
    }

    private static void emit(VertexConsumer lines, PoseStack.Pose pose, double[] seg, Vec3 cam, int argb, int alpha) {
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
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
            lines.addVertex(pose, x1, y1, z1).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
            lines.addVertex(pose, x2, y2, z2).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        }
    }
}
