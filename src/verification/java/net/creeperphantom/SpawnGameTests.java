package net.creeperphantom;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreeperPhantomMod.ID)
@PrefixGameTestTemplate(false)
public final class SpawnGameTests {
    private static Mob natural(GameTestHelper h, EntityType<? extends Mob> type, BlockPos pos) {
        Mob mob = type.create(h.getLevel());
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        ForgeEventFactory.onFinalizeSpawn(mob, h.getLevel(), h.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        return mob;
    }

    @GameTest(template = "empty", batch = "spawnConfig", timeoutTicks = 100)
    public static void switchesAndMultipliers(GameTestHelper h) {
        // Exercise runtime values without racing Forge's on-disk configuration watcher.
        Config.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        double oldP = Config.REPLACEMENT_CHANCE.get(), oldE = Config.ENDER_SPAWN_MULTIPLIER.get();
        boolean oldPE = Config.PHANTOM_NATURAL_SPAWNS.get(), oldEE = Config.ENDER_NATURAL_SPAWNS.get();
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(6, 264, 6));
        AABB area = new AABB(pos).inflate(12);
        try {
            var random = RandomSource.create(418);
            int ones = 0, twos = 0;
            for (int i = 0; i < 1000; i++) {
                h.assertTrue(NaturalSpawns.spawnCount(0, random) == 0, "Zero multiplier");
                h.assertTrue(NaturalSpawns.spawnCount(2, random) == 2, "Integer multiplier greater than one");
                int count = NaturalSpawns.spawnCount(1.5, random);
                h.assertTrue(count == 1 || count == 2, "Fractional multiplier stays within floor/ceil");
                if (count == 1) ones++; else twos++;
            }
            h.assertTrue(ones > 400 && twos > 400, "Fractional part must generate both outcomes with fixed seed");
            Config.PHANTOM_NATURAL_SPAWNS.set(false);
            Config.REPLACEMENT_CHANCE.set(2.0);
            Mob untouched = natural(h, EntityType.PHANTOM, pos);
            h.assertTrue(level.addFreshEntity(untouched), "Disabled phantom replacement must preserve vanilla spawn");
            untouched.discard();
            Config.PHANTOM_NATURAL_SPAWNS.set(true);
            int before = level.getEntitiesOfClass(CreeperPhantom.class, area).size();
            h.assertFalse(level.addFreshEntity(natural(h, EntityType.PHANTOM, pos)), "Multiplier replaces original");
            h.assertTrue(level.getEntitiesOfClass(CreeperPhantom.class, area).size() == before + 2,
                    "Multiplier two must really add two phantoms when space exists");
            Config.ENDER_NATURAL_SPAWNS.set(false);
            Config.ENDER_SPAWN_MULTIPLIER.set(1.0);
            Mob enderman = natural(h, EntityType.ENDERMAN, pos);
            h.assertTrue(level.addFreshEntity(enderman), "Disabled ender replacement must preserve vanilla spawn");
            enderman.discard();
            EnderCreeper commanded = CreeperPhantomMod.ENDER_CREEPER.get().create(level);
            commanded.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
            h.assertTrue(level.addFreshEntity(commanded), "Natural spawn toggle must not prohibit custom commands or eggs");
            commanded.discard();
            Config.ENDER_NATURAL_SPAWNS.set(true);
            before = level.getEntitiesOfClass(EnderCreeper.class, area).size();
            h.assertFalse(level.addFreshEntity(natural(h, EntityType.ENDERMAN, pos)), "Enderman natural spawn must be replaceable");
            h.assertTrue(level.getEntitiesOfClass(EnderCreeper.class, area).size() == before + 1, "One ender replacement is produced");
            Config.ENDER_SPAWN_MULTIPLIER.set(0.0);
            Mob zero = natural(h, EntityType.ENDERMAN, pos);
            h.assertTrue(level.addFreshEntity(zero), "Zero multiplier must preserve original Enderman");
            zero.discard();
            h.succeed();
        } finally {
            level.getEntitiesOfClass(CreeperPhantom.class, area).forEach(CreeperPhantom::discard);
            level.getEntitiesOfClass(EnderCreeper.class, area).forEach(EnderCreeper::discard);
            Config.REPLACEMENT_CHANCE.set(oldP);
            Config.ENDER_SPAWN_MULTIPLIER.set(oldE);
            Config.PHANTOM_NATURAL_SPAWNS.set(oldPE);
            Config.ENDER_NATURAL_SPAWNS.set(oldEE);
        }
    }
}
