package net.creeperphantom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreeperPhantomMod.ID)
@PrefixGameTestTemplate(false)
public final class EnderGameTests {
    private static Goal goal(EnderCreeper mob, String name) throws ReflectiveOperationException {
        Field f = Mob.class.getDeclaredField("goalSelector");
        f.setAccessible(true);
        return ((GoalSelector) f.get(mob)).getAvailableGoals().stream().map(g -> g.getGoal())
                .filter(g -> g.getClass().getSimpleName().equals(name)).findFirst().orElseThrow();
    }

    @GameTest(template = "empty", batch = "enderBehavior", timeoutTicks = 200)
    public static void neutralTeleportAndAutonomousRaids(GameTestHelper helper) {
        Config.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        helper.setNight();
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) {
            helper.setBlock(x, 260, z, Blocks.STONE);
            for (int y = 261; y <= 266; y++) helper.setBlock(x, y, z, Blocks.AIR);
        }
        helper.runAfterDelay(20, () -> exercise(helper));
    }

    private static void exercise(GameTestHelper helper) {
        var level = helper.getLevel();
        double oldChance = Config.CONTAINER_CHANCE.get();
        boolean oldGt = Config.GT_MACHINE_RAIDS.get();
        var grief = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean oldGrief = grief.get();
        List<EnderCreeper> mobs = new ArrayList<>();
        List<Float> powers = new ArrayList<>();
        Consumer<ExplosionEvent.Start> explosions = event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof EnderCreeper) {
                try {
                    Field radius = net.minecraft.world.level.Explosion.class.getDeclaredField("radius");
                    radius.setAccessible(true);
                    powers.add(radius.getFloat(event.getExplosion()));
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                event.setCanceled(true);
            }
        };
        Consumer<EntityTeleportEvent.EnderEntity> cancel = event -> {
            if (event.getEntity() instanceof EnderCreeper) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(explosions);
        var player = helper.makeMockSurvivalPlayer();
        try {
            Vec3 playerPos = helper.absoluteVec(new Vec3(9.5, 261, 9.5));
            player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0, 0);
            Config.CONTAINER_CHANCE.set(1.0);
            Config.GT_MACHINE_RAIDS.set(false);
            grief.set(true, level.getServer());
            var mob = helper.spawn(CreeperPhantomMod.ENDER_CREEPER.get(), 3, 261, 3);
            mobs.add(mob);
            mob.setNoAi(true);
            helper.assertFalse(mob.isPowered(), "Ordinary ender form starts uncharged");
            helper.assertTrue(mob.isSensitiveToWater(), "Enderman water sensitivity must remain");
            helper.assertTrue(mob.getTarget() == null && !goal(mob, "TeleportAttackGoal").canUse(),
                    "Nearby player alone must not trigger a player attack");
            var stare = EnderMan.class.getDeclaredMethod("isLookingAtMe", Player.class);
            stare.setAccessible(true);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
            helper.assertTrue((boolean) stare.invoke(mob, player), "Vanilla staring predicate must recognize this mob");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.CARVED_PUMPKIN));
            helper.assertFalse((boolean) stare.invoke(mob, player), "Pumpkin must still prevent stare provocation");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

            helper.setBlock(8, 261, 8, Blocks.CHEST);
            Goal raid = goal(mob, "TeleportBlockGoal");
            grief.set(false, level.getServer());
            helper.assertFalse(raid.canUse(), "Block teleport raids obey mobGriefing");
            grief.set(true, level.getServer());
            mob.tickCount += 300;
            helper.assertTrue(raid.canUse(), "Unprovoked mob must independently acquire an exposed chest");
            Vec3 before = mob.position();
            raid.start();
            helper.assertTrue(mob.position().distanceToSqr(before) > 1 && mob.isPrimed(), "Successful chest teleport must ignite");
            helper.assertTrue(mob.getTarget() == null, "Container raid must not manufacture player anger");
            helper.assertFalse(level.containsAnyLiquid(mob.getBoundingBox()), "Landing must be dry");
            helper.assertTrue(level.noCollision(mob), "Landing must fit full Enderman height");
            CompoundTag tag = new CompoundTag();
            mob.addAdditionalSaveData(tag);
            var restored = CreeperPhantomMod.ENDER_CREEPER.get().create(level);
            restored.readAdditionalSaveData(tag);
            helper.assertTrue(restored.isPrimed() && restored.fuseRemaining() == mob.fuseRemaining(), "Fuse persists through NBT");
            for (int i = 0; i < Config.FUSE_TICKS.get() - 1; i++) mob.aiStep();
            helper.assertFalse(mob.isRemoved(), "Ordinary fuse must not detonate early");
            mob.aiStep();
            helper.assertTrue(mob.isRemoved() && powers.equals(List.of(3F)), "Ordinary teleport attack ends in power-3 explosion");

            var attacker = helper.spawn(CreeperPhantomMod.ENDER_CREEPER.get(), 2, 261, 2);
            mobs.add(attacker);
            attacker.setNoAi(true);
            attacker.setTarget(player);
            Goal attack = goal(attacker, "TeleportAttackGoal");
            MinecraftForge.EVENT_BUS.addListener(cancel);
            Vec3 original = attacker.position();
            helper.assertTrue(attack.canUse(), "Provoked entity must attempt a teleport attack");
            attack.start();
            helper.assertTrue(!attacker.isPrimed() && attacker.position().equals(original), "Canceled teleport must neither move nor ignite");
            MinecraftForge.EVENT_BUS.unregister(cancel);
            attacker.tickCount += 60;
            helper.assertTrue(attack.canUse(), "Failed teleport may retry after cooldown");
            attack.start();
            helper.assertTrue(attacker.isPrimed() && attacker.distanceToSqr(player) < 25, "Angry mob must teleport next to player and ignite");
            attacker.discard();

            helper.assertTrue(((SpawnEggItem) CreeperPhantomMod.ENDER_SPAWN_EGG.get()).getType(null) == CreeperPhantomMod.ENDER_CREEPER.get(), "Normal ender egg");
            helper.assertTrue(((SpawnEggItem) CreeperPhantomMod.CHARGED_ENDER_SPAWN_EGG.get()).getType(null) == CreeperPhantomMod.CHARGED_ENDER_CREEPER.get(), "Charged ender egg");
            var charged = helper.spawn(CreeperPhantomMod.CHARGED_ENDER_CREEPER.get(), 2, 261, 2);
            mobs.add(charged);
            charged.setNoAi(true);
            charged.setTarget(player);
            goal(charged, "TeleportAttackGoal").start();
            helper.assertTrue(charged.isRemoved() && powers.equals(List.of(3F, 6F)), "Charged teleport must instantly explode with power 6");
            var lightningMob = helper.spawn(CreeperPhantomMod.ENDER_CREEPER.get(), 3, 261, 3);
            mobs.add(lightningMob);
            lightningMob.setNoAi(true);
            lightningMob.thunderHit(level, EntityType.LIGHTNING_BOLT.create(level));
            helper.assertTrue(lightningMob.isPowered(), "Lightning must naturally charge the ender form");
            lightningMob.addAdditionalSaveData(tag);
            restored.readAdditionalSaveData(tag);
            helper.assertTrue(restored.isPowered(), "Ender charge persists through NBT");
            var safe = EnderCreeper.class.getDeclaredMethod("safeLanding", Vec3.class);
            safe.setAccessible(true);
            Vec3 landing = helper.absoluteVec(new Vec3(5.5, 261, 5.5));
            helper.setBlock(5, 262, 5, Blocks.STONE);
            helper.assertFalse((boolean) safe.invoke(lightningMob, landing), "Low ceiling must reject teleport destination");
            helper.setBlock(5, 262, 5, Blocks.AIR);
            helper.setBlock(5, 261, 5, Blocks.WATER);
            helper.assertFalse((boolean) safe.invoke(lightningMob, landing), "Water must reject teleport destination");
            helper.setBlock(5, 261, 5, Blocks.AIR);
            helper.succeed();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        finally {
            MinecraftForge.EVENT_BUS.unregister(explosions);
            MinecraftForge.EVENT_BUS.unregister(cancel);
            mobs.forEach(EnderCreeper::discard);
            player.discard();
            helper.setBlock(8, 261, 8, Blocks.AIR);
            helper.setBlock(5, 261, 5, Blocks.AIR);
            helper.setBlock(5, 262, 5, Blocks.AIR);
            Config.CONTAINER_CHANCE.set(oldChance);
            Config.GT_MACHINE_RAIDS.set(oldGt);
            grief.set(oldGrief, level.getServer());
        }
    }
}
