package net.creeperphantom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreeperPhantomMod.ID)
public final class NaturalSpawns {
    private static final String NATURAL = CreeperPhantomMod.ID + ":natural_candidate";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finalized(MobSpawnEvent.FinalizeSpawn event) {
        if (!event.isSpawnCancelled() && event.getSpawnType() == MobSpawnType.NATURAL
                && event.getEntity().getType() == EntityType.PHANTOM) {
            event.getEntity().getPersistentData().putBoolean(NATURAL, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || event.getEntity().getType() != EntityType.PHANTOM
                || !(event.getEntity() instanceof Phantom original)) return;
        if (!original.getPersistentData().getBoolean(NATURAL)) return;
        original.getPersistentData().remove(NATURAL);
        if (level.random.nextDouble() >= Config.REPLACEMENT_CHANCE.get()) return;
        CreeperPhantom replacement = CreeperPhantomMod.PHANTOM.get().create(level);
        if (replacement == null) return;
        replacement.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        ForgeEventFactory.onFinalizeSpawn(replacement, level, level.getCurrentDifficultyAt(replacement.blockPosition()),
                MobSpawnType.NATURAL, null, null);
        replacement.setPhantomSize(original.getPhantomSize());
        // If another mod prevents the replacement spawn, leave the original spawn intact.
        if (!replacement.isSpawnCancelled() && level.addFreshEntity(replacement)) event.setCanceled(true);
    }
}
