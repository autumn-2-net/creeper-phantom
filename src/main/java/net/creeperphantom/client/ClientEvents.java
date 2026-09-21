package net.creeperphantom.client;

import net.creeperphantom.CreeperPhantomMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreeperPhantomMod.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CreeperPhantomMod.PHANTOM.get(), CreeperPhantomRenderer::new);
        event.registerEntityRenderer(CreeperPhantomMod.CHARGED_PHANTOM.get(), CreeperPhantomRenderer::new);
        event.registerEntityRenderer(CreeperPhantomMod.ENDER_CREEPER.get(), EnderCreeperRenderer::new);
        event.registerEntityRenderer(CreeperPhantomMod.CHARGED_ENDER_CREEPER.get(), EnderCreeperRenderer::new);
    }
}
