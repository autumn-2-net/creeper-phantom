package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.creeperphantom.EnderCreeper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import static net.creeperphantom.client.AtlasMesh.box;

public final class EnderCreeperModel extends EntityModel<EnderCreeper> {
    private static final float[] FACE = {0, 0, 0.5F, 0.5F};
    private static final float[] SKIN = {0.5F, 0, 1, 0.5F};
    private static final float[] BELLY = {0, 0.5F, 0.5F, 1};
    private static final float[] LIMBS = {0.5F, 0.5F, 1, 1};
    private final float scale;
    private float yaw, pitch, swing;
    private boolean carrying, angry;

    public EnderCreeperModel() { this(1); }
    public EnderCreeperModel(float scale) { this.scale = scale; }

    @Override public void setupAnim(EnderCreeper mob, float walk, float speed, float age, float yaw, float pitch) {
        this.yaw = yaw * Mth.DEG_TO_RAD;
        this.pitch = pitch * Mth.DEG_TO_RAD;
        swing = Mth.cos(walk * 0.6662F) * speed * 0.6F;
        carrying = mob.getCarriedBlock() != null;
        angry = mob.isCreepy();
    }

    private void headPose(PoseStack pose) {
        pose.translate(0, angry ? -16 : -15, 0);
        pose.mulPose(Axis.YP.rotation(yaw));
        pose.mulPose(Axis.XP.rotation(pitch));
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, float r, float g, float b, float alpha) {
        pose.pushPose();
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        pose.translate(0, 24, 0);
        pose.scale(scale, scale, scale);
        pose.translate(0, -24, 0);
        box(pose, out, -4, -15, -2, 4, -3, 2, SKIN, BELLY, light, overlay, r, g, b, alpha);
        pose.pushPose();
        headPose(pose);
        box(pose, out, -4, -8, -4, 4, 0, 4, SKIN, FACE, light, overlay, r, g, b, alpha);
        pose.popPose();
        for (int side : new int[]{-1, 1}) {
            pose.pushPose();
            pose.translate(side * 5, -14, 0);
            pose.mulPose(Axis.XP.rotation(carrying ? -0.9F : side * swing));
            box(pose, out, -1, -1, -1, 1, 29, 1, LIMBS, LIMBS, light, overlay, r, g, b, alpha);
            pose.popPose();
            pose.pushPose();
            pose.translate(side * 2, -3, 0);
            pose.mulPose(Axis.XP.rotation(-side * swing));
            box(pose, out, -1, 0, -1, 1, 27, 1, LIMBS, LIMBS, light, overlay, r, g, b, alpha);
            pose.popPose();
        }
        pose.popPose();
    }

    public void renderEyes(PoseStack pose, VertexConsumer out, int overlay) {
        pose.pushPose();
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        headPose(pose);
        // Sample the two purple stripes from the generated atlas, without a second edited bitmap.
        for (int side : new int[]{-1, 1}) {
            float x0 = side < 0 ? -3.333F : 0.667F, x1 = side < 0 ? -0.667F : 3.333F;
            float u0 = side < 0 ? 7F / 24 : 1F / 24, u1 = side < 0 ? 11F / 24 : 5F / 24;
            AtlasMesh.quad(pose, out, new float[]{x1,-4.667F,-4.01F, x0,-4.667F,-4.01F,
                    x0,-4,-4.01F, x1,-4,-4.01F}, new float[]{u0,5F/24,u1,6F/24},
                    0, 0, -1, 15728880, overlay, 1, 1, 1, 1);
        }
        pose.popPose();
    }
}
