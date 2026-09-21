package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.creeperphantom.CreeperPhantom;
import net.creeperphantom.CreeperPhantomMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;

public final class CreeperPhantomRenderer extends MobRenderer<CreeperPhantom, CreeperPhantomModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CreeperPhantomMod.ID, "textures/entity/creeper_phantom.png");

    public CreeperPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperPhantomModel(), 0.65F);
        addLayer(new EnergySwirlLayer<CreeperPhantom, CreeperPhantomModel>(this) {
            private final CreeperPhantomModel chargedModel = new CreeperPhantomModel(1.025F);

            @Override
            protected float xOffset(float age) { return age * 0.01F; }

            @Override
            protected ResourceLocation getTextureLocation() {
                return new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");
            }

            @Override
            protected EntityModel<CreeperPhantom> model() { return chargedModel; }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(CreeperPhantom entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(CreeperPhantom entity, PoseStack pose, float age, float yaw, float partialTick) {
        super.setupRotations(entity, pose, age, yaw, partialTick);
        pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }

    @Override
    protected void scale(CreeperPhantom entity, PoseStack pose, float partialTick) {
        float size = 1.0F + entity.getPhantomSize() * 0.15F;
        if (entity.isPrimed()) size *= 1.08F + 0.04F * (float) Math.sin((entity.tickCount + partialTick) * 1.7F);
        pose.scale(size, size, size);
        // The renderer's living-entity translation places model Y=24 at the feet.
        pose.translate(0, 1.3125, 0);
    }

    @Override
    protected float getWhiteOverlayProgress(CreeperPhantom entity, float partialTick) {
        return entity.isPrimed() && ((entity.fuseRemaining() / 3) & 1) == 0 ? 0.9F : 0;
    }
}
