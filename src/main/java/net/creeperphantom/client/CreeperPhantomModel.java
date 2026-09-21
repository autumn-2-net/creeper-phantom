package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.creeperphantom.CreeperPhantom;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;

/** Cuboid mesh with four explicit UV tiles, matching the generated atlas without raster editing. */
public final class CreeperPhantomModel extends EntityModel<CreeperPhantom> {
    private float flap;
    private float tail;
    private final float meshScale;

    public CreeperPhantomModel() { this(1.0F); }

    public CreeperPhantomModel(float meshScale) { this.meshScale = meshScale; }
    private static final float[] FACE = {0, 0, 0.5F, 0.5F};
    private static final float[] SKIN = {0.5F, 0, 1, 0.5F};
    private static final float[] WING = {0, 0.5F, 0.5F, 1};
    private static final float[] TAIL = {0.5F, 0.5F, 1, 1};

    @Override
    public void setupAnim(CreeperPhantom entity, float walk, float speed, float age, float yaw, float pitch) {
        float phase = (age + entity.getUniqueFlapTickOffset()) * 7.448451F * Mth.DEG_TO_RAD;
        flap = Mth.cos(phase) * 0.32F;
        tail = Mth.cos(phase) * 0.06F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay,
                               float r, float g, float b, float alpha) {
        pose.pushPose();
        pose.scale(meshScale / 16, meshScale / 16, meshScale / 16);
        box(pose, out, -4, -3, -10, 4, 2, 11, SKIN, SKIN, light, overlay, r, g, b, alpha);
        box(pose, out, -4, -4, -17, 4, 4, -9, SKIN, FACE, light, overlay, r, g, b, alpha);
        for (int side : new int[]{-1, 1}) {
            pose.pushPose();
            pose.translate(side * 3.5F, 0, -2);
            pose.mulPose(Axis.ZP.rotation(side * flap));
            // Two overlapping sections form the stepped silhouette of a phantom wing.
            float lo = side < 0 ? -13 : 0, hi = side < 0 ? 0 : 13;
            box(pose, out, lo, -0.5F, -3, hi, 0.5F, 9, WING, WING, light, overlay, r, g, b, alpha);
            pose.translate(side * 12, 0, 1);
            pose.mulPose(Axis.ZP.rotation(side * flap * 0.5F));
            lo = side < 0 ? -13 : 0;
            hi = side < 0 ? 0 : 13;
            box(pose, out, lo, -0.45F, -2, hi, 0.45F, 7, WING, WING, light, overlay, r, g, b, alpha);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0, 0, 10);
        pose.mulPose(Axis.XP.rotation(tail));
        box(pose, out, -2, -0.5F, 0, 2, 0.5F, 15, TAIL, TAIL, light, overlay, r, g, b, alpha);
        pose.popPose();
        pose.popPose();
    }

    private static void box(PoseStack pose, VertexConsumer out, float x0, float y0, float z0,
                            float x1, float y1, float z1, float[] uv, float[] front,
                            int light, int overlay, float r, float g, float b, float a) {
        quad(pose, out, new float[]{x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0}, front, 0,0,-1, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1}, uv, 0,0,1, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0}, uv, -1,0,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1}, uv, 1,0,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1}, uv, 0,-1,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0}, uv, 0,1,0, light,overlay,r,g,b,a);
    }

    private static void quad(PoseStack pose, VertexConsumer out, float[] xyz, float[] uv,
                             float nx, float ny, float nz, int light, int overlay,
                             float r, float g, float b, float a) {
        var transform = pose.last();
        // Keep UV samples just inside their quadrant to avoid edge bleeding.
        float inset = 0.0005F;
        float[] us = {uv[0]+inset, uv[2]-inset, uv[2]-inset, uv[0]+inset};
        float[] vs = {uv[1]+inset, uv[1]+inset, uv[3]-inset, uv[3]-inset};
        for (int i = 0; i < 4; i++) {
            out.vertex(transform.pose(), xyz[i*3], xyz[i*3+1], xyz[i*3+2])
                    .color(r,g,b,a).uv(us[i],vs[i]).overlayCoords(overlay).uv2(light)
                    .normal(transform.normal(),nx,ny,nz).endVertex();
        }
    }
}
