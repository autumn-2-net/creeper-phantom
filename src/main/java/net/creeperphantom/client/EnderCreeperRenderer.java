package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.creeperphantom.CreeperPhantomMod;
import net.creeperphantom.EnderCreeper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class EnderCreeperRenderer extends MobRenderer<EnderCreeper, EnderCreeperModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CreeperPhantomMod.ID, "textures/entity/ender_creeper.png");

    public EnderCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new EnderCreeperModel(), 0.5F);
        addLayer(new RenderLayer<EnderCreeper, EnderCreeperModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EnderCreeper mob,
                                         float walk, float speed, float partial, float age, float yaw, float pitch) {
                if (!mob.isInvisible()) getParentModel().renderEyes(pose, buffers.getBuffer(RenderType.eyes(TEXTURE)), OverlayTexture.NO_OVERLAY);
                if (mob.getCarriedBlock() != null && !mob.isInvisible()) {
                    pose.pushPose();
                    pose.translate(0, -0.15, -0.45);
                    pose.mulPose(Axis.XP.rotationDegrees(20));
                    pose.scale(-0.45F, -0.45F, 0.45F);
                    pose.translate(-0.5, 0, -0.5);
                    context.getBlockRenderDispatcher().renderSingleBlock(mob.getCarriedBlock(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        });
        addLayer(new EnergySwirlLayer<EnderCreeper, EnderCreeperModel>(this) {
            private final EnderCreeperModel aura = new EnderCreeperModel(1.025F);
            @Override protected float xOffset(float age) { return age * 0.01F; }
            @Override protected ResourceLocation getTextureLocation() { return new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png"); }
            @Override protected EntityModel<EnderCreeper> model() { return aura; }
        });
    }

    @Override public ResourceLocation getTextureLocation(EnderCreeper mob) { return TEXTURE; }
    @Override protected float getWhiteOverlayProgress(EnderCreeper mob, float partial) {
        return mob.isPrimed() && ((mob.fuseRemaining() / 3) & 1) == 0 ? 0.9F : 0;
    }
}
