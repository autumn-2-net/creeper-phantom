package net.creeperphantom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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
                && (event.getEntity().getType() == EntityType.PHANTOM || event.getEntity().getType() == EntityType.ENDERMAN)) {
            event.getEntity().getPersistentData().putBoolean(NATURAL, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob original)) return;
        boolean phantom = original.getType() == EntityType.PHANTOM;
        if (!phantom && original.getType() != EntityType.ENDERMAN) return;
        if (!original.getPersistentData().getBoolean(NATURAL)) return;
        original.getPersistentData().remove(NATURAL);
        if (!(phantom ? Config.PHANTOM_NATURAL_SPAWNS : Config.ENDER_NATURAL_SPAWNS).get()) return;
        int count = spawnCount((phantom ? Config.REPLACEMENT_CHANCE : Config.ENDER_SPAWN_MULTIPLIER).get(), level.random);
        boolean added = false;
        for (int i = 0; i < count; i++) {
            Mob replacement = phantom ? CreeperPhantomMod.PHANTOM.get().create(level) : CreeperPhantomMod.ENDER_CREEPER.get().create(level);
            if (replacement == null) continue;
            replacement.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
            if (i > 0 && !placeExtra(level, original, replacement, phantom)) continue;
            ForgeEventFactory.onFinalizeSpawn(replacement, level, level.getCurrentDifficultyAt(replacement.blockPosition()),
                    MobSpawnType.NATURAL, null, null);
            if (replacement instanceof Phantom newPhantom && original instanceof Phantom oldPhantom) newPhantom.setPhantomSize(oldPhantom.getPhantomSize());
            if (!replacement.isSpawnCancelled() && level.addFreshEntity(replacement)) added = true;
        }
        // Never delete the original unless at least one replacement was actually accepted.
        if (added) event.setCanceled(true);
    }

    static int spawnCount(double multiplier, RandomSource random) {
        if (!Double.isFinite(multiplier) || multiplier <= 0) return 0;
        double bounded = Math.min(64, multiplier);
        int whole = (int) bounded;
        return whole + (random.nextDouble() < bounded - whole ? 1 : 0);
    }

    private static boolean placeExtra(ServerLevel level, Mob original, Mob extra, boolean phantom) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double x = original.getX() + level.random.nextInt(11) - 5;
            double z = original.getZ() + level.random.nextInt(11) - 5;
            double y = original.getY() + level.random.nextInt(5) - 2;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) continue;
            if (!phantom && !SpawnPlacements.checkSpawnRules(EntityType.ENDERMAN, level, MobSpawnType.NATURAL, pos, level.random)) continue;
            extra.moveTo(x, y, z, original.getYRot(), original.getXRot());
            if (level.noCollision(extra) && !level.containsAnyLiquid(extra.getBoundingBox())
                    && level.getEntities(extra, extra.getBoundingBox()).isEmpty()) return true;
        }
        return false;
    }
}
