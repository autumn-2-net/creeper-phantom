package net.creeperphantom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreeperPhantomMod.ID)
@PrefixGameTestTemplate(false)
public final class PhantomGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void formsEggsLightningAndFuses(GameTestHelper helper) {
        helper.setNight();
        helper.assertTrue(((SpawnEggItem) CreeperPhantomMod.SPAWN_EGG.get()).getType(null)
                == CreeperPhantomMod.PHANTOM.get(), "Normal egg type");
        helper.assertTrue(((SpawnEggItem) CreeperPhantomMod.CHARGED_SPAWN_EGG.get()).getType(null)
                == CreeperPhantomMod.CHARGED_PHANTOM.get(), "Charged egg type");
        var ordinary = helper.spawnWithNoFreeWill(CreeperPhantomMod.PHANTOM.get(), 3, 3, 3);
        var charged = helper.spawnWithNoFreeWill(CreeperPhantomMod.CHARGED_PHANTOM.get(), 6, 3, 3);
        helper.assertFalse(ordinary.isPowered(), "Normal spawn must not be charged");
        helper.assertTrue(charged.isPowered(), "Charged egg/entity must start charged");
        var lightning = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        ordinary.thunderHit(helper.getLevel(), lightning);
        helper.assertTrue(ordinary.isPowered(), "Lightning must charge an ordinary phantom");
        var tag = new CompoundTag();
        ordinary.addAdditionalSaveData(tag);
        var restored = CreeperPhantomMod.PHANTOM.get().create(helper.getLevel());
        restored.readAdditionalSaveData(tag);
        helper.assertTrue(restored.isPowered(), "Charge must survive saving and loading");
        ordinary.discard();

        var target = helper.spawnWithNoFreeWill(EntityType.PIG, 6, 3, 4);
        List<Float> powers = new ArrayList<>();
        Consumer<ExplosionEvent.Start> capture = event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof CreeperPhantom) {
                try {
                    var radius = net.minecraft.world.level.Explosion.class.getDeclaredField("radius");
                    radius.setAccessible(true);
                    powers.add(radius.getFloat(event.getExplosion()));
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(capture);
        try {
            helper.assertTrue(charged.doHurtTarget(target), "Charged contact must attack");
            helper.assertTrue(charged.isRemoved(), "Charged contact must detonate immediately");
            helper.assertTrue(powers.size() == 1 && powers.get(0) == 6F, "Charged explosion must have power 6");
            charged.doHurtTarget(target);
            helper.assertTrue(powers.size() == 1, "One entity must never explode twice");
            var normal = helper.spawnWithNoFreeWill(CreeperPhantomMod.PHANTOM.get(), 3, 3, 4);
            normal.doHurtTarget(target);
            helper.assertTrue(normal.isPrimed() && !normal.isRemoved(), "Normal contact must start a fuse");
            helper.assertTrue(powers.size() == 1, "Normal contact must not instantly explode");
            for (int i = 0; i < Config.FUSE_TICKS.get() - 1; i++) normal.aiStep();
            helper.assertFalse(normal.isRemoved(), "Normal fuse must not explode early");
            normal.aiStep();
            helper.assertTrue(normal.isRemoved() && powers.size() == 2 && powers.get(1) == 3F,
                    "Normal fuse must finish with exactly one power-3 explosion");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(capture);
        }
        checkNaturalReplacement(helper);
        // The sky-light engine applies freshly placed test structures asynchronously.
        helper.runAfterDelay(15, () -> {
            checkContainersAndGriefing(helper);
            helper.succeed();
        });
    }

    private static void checkNaturalReplacement(GameTestHelper helper) {
        double oldChance = Config.REPLACEMENT_CHANCE.get();
        Config.REPLACEMENT_CHANCE.set(1.0);
        try {
            var level = helper.getLevel();
            var pos = helper.absolutePos(new BlockPos(9, 4, 9));
            var bounds = new net.minecraft.world.phys.AABB(pos).inflate(1);
            int before = level.getEntitiesOfClass(CreeperPhantom.class, bounds).size();
            var original = EntityType.PHANTOM.create(level);
            original.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
            net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(original, level,
                    level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.NATURAL, null, null);
            helper.assertFalse(level.addFreshEntity(original), "Replaced vanilla spawn must be cancelled");
            helper.assertTrue(level.getEntitiesOfClass(CreeperPhantom.class, bounds).size() == before + 1,
                    "Natural spawn must add exactly one custom phantom");
            var summoned = EntityType.PHANTOM.create(level);
            summoned.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
            net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(summoned, level,
                    level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
            helper.assertTrue(level.addFreshEntity(summoned), "Command phantom must not be replaced");
            summoned.discard();
            level.getEntitiesOfClass(CreeperPhantom.class, bounds).forEach(CreeperPhantom::discard);
        } finally { Config.REPLACEMENT_CHANCE.set(oldChance); }
    }

    private static void checkContainersAndGriefing(GameTestHelper helper) {
        var level = helper.getLevel();
        var grief = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean oldGrief = grief.get();
        double oldChance = Config.CONTAINER_CHANCE.get();
        Config.CONTAINER_CHANCE.set(1.0);
        grief.set(true, level.getServer());
        try {
            // Forge 1.20.1 hardcodes the structure origin at Y=-60, even with --spawnPos.
            // Place the outdoor scenario at absolute Y=201, above the generated terrain.
            var chest = new BlockPos(7, 261, 7);
            helper.setBlock(chest, net.minecraft.world.level.block.Blocks.CHEST);
            var raider = helper.spawn(CreeperPhantomMod.PHANTOM.get(), 3, 264, 7);
            raider.setNoAi(true);
            var field = net.minecraft.world.entity.Mob.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            var goals = (net.minecraft.world.entity.ai.goal.GoalSelector) field.get(raider);
            var raid = goals.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                    .filter(goal -> goal.getClass().getSimpleName().equals("BombContainerGoal")).findFirst().orElseThrow();
            var inventoryCheck = CreeperPhantom.class.getDeclaredMethod("inventoryAt", BlockPos.class);
            var exposedCheck = CreeperPhantom.class.getDeclaredMethod("exposed", BlockPos.class);
            inventoryCheck.setAccessible(true);
            exposedCheck.setAccessible(true);
            var absoluteChest = helper.absolutePos(chest);
            helper.assertTrue((boolean) inventoryCheck.invoke(raider, absoluteChest), "Chest must be recognized as an inventory");
            helper.assertTrue((boolean) exposedCheck.invoke(raider, absoluteChest), "Chest must be exposed: sky="
                    + level.canSeeSky(absoluteChest.above()) + " line="
                    + level.clip(new net.minecraft.world.level.ClipContext(raider.getEyePosition(),
                    net.minecraft.world.phys.Vec3.atCenterOf(absoluteChest).add(0, 0.8, 0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, raider)));
            helper.assertTrue(raid.canUse(), "Exposed chest should be acquired at 100 percent chance: chance="
                    + Config.CONTAINER_CHANCE.get() + " grief=" + grief.get() + " chunks="
                    + (level.getChunkSource().getChunkNow(absoluteChest.getX() >> 4, absoluteChest.getZ() >> 4) != null));
            raid.start();
            helper.assertTrue(raid.canContinueToUse(), "Visible chest raid should continue");
            helper.setBlock(chest.above(), net.minecraft.world.level.block.Blocks.STONE);
            helper.assertFalse(raid.canContinueToUse(), "Roof must invalidate exposed-container target");
            raid.stop();
            raider.tickCount += 300;
            helper.assertFalse(raid.canUse(), "Roofed chest must not be acquired");
            helper.setBlock(chest.above(), net.minecraft.world.level.block.Blocks.AIR);
            raider.tickCount += 300;
            grief.set(false, level.getServer());
            helper.assertFalse(raid.canUse(), "mobGriefing=false must disable container raids");
            raider.discard();

            var charged = helper.spawnWithNoFreeWill(CreeperPhantomMod.CHARGED_PHANTOM.get(), 7, 262, 7);
            var target = helper.spawnWithNoFreeWill(EntityType.PIG, 7, 262, 8);
            charged.doHurtTarget(target);
            helper.assertTrue(charged.isRemoved(), "mobGriefing=false must still allow detonation");
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.CHEST, chest);
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Protected blocks must not disable entity damage");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        finally {
            Config.CONTAINER_CHANCE.set(oldChance);
            grief.set(oldGrief, level.getServer());
        }
    }
}
