package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import static net.creeperphantom.client.AtlasMesh.box;
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

}
