package net.creeperphantom;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Uses a small API-shaped fixture, not a bundled or emulated copy of GTCEu. */
@GameTestHolder(CreeperPhantomMod.ID)
@PrefixGameTestTemplate(false)
public final class GtRaidGameTests {
    public interface HolderApi { Object getMetaMachine(); }
    public interface ControllerApi { boolean isFormed(); }

    public static final class Controller implements ControllerApi {
        boolean formed;
        @Override public boolean isFormed() { return formed; }
    }

    public static final class Holder extends ChestBlockEntity implements HolderApi {
        Object machine;
        Holder(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public Object getMetaMachine() { return machine; }
    }

    @GameTest(template = "empty", batch = "gtRaids", timeoutTicks = 100)
    public static void controllerPriorityAndFormation(GameTestHelper helper) {
        Config.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        helper.setNight();
        helper.runAfterDelay(15, () -> exercise(helper));
    }

    private static WrappedGoal find(GoalSelector selector, String name) {
        return selector.getAvailableGoals().stream()
                .filter(goal -> goal.getGoal().getClass().getSimpleName().equals(name)).findFirst().orElseThrow();
    }

    private static void exercise(GameTestHelper helper) {
        var level = helper.getLevel();
        var grief = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean oldGrief = grief.get(), oldEnabled = Config.GT_MACHINE_RAIDS.get();
        double oldChance = Config.GT_MACHINE_CHANCE.get();
        Field access = null;
        Object originalAdapter = null;
        CreeperPhantom raider = null;
        EnderCreeper ender = null;
        BlockPos controllerPos = helper.absolutePos(new BlockPos(7, 261, 7));
        try {
            // This test run has no GT installation: first verify the optional bridge is inactive.
            helper.assertFalse(GtMultiblocks.available(), "Standalone installation must not require GT");
            var fixture = GtMultiblocks.bind(HolderApi.class, ControllerApi.class);
            access = GtMultiblocks.class.getDeclaredField("adapter");
            access.setAccessible(true);
            originalAdapter = access.get(null);
            access.set(null, fixture);
            grief.set(true, level.getServer());
            Config.GT_MACHINE_RAIDS.set(true);
            Config.GT_MACHINE_CHANCE.set(1.0);
            level.setBlockAndUpdate(controllerPos, Blocks.CHEST.defaultBlockState());
            // A controller with inventory slots catches accidental reuse of the 10% container path.
            var holder = new Holder(controllerPos, level.getBlockState(controllerPos));
            var controller = new Controller();
            holder.machine = controller;
            level.setBlockEntity(holder);
            helper.assertTrue(GtMultiblocks.classify(holder) == GtMultiblocks.Kind.UNFORMED_CONTROLLER,
                    "Unformed controllers must remain identifiable but ineligible");
            helper.assertTrue(fixture.classify(new Object()) == GtMultiblocks.Kind.OTHER, "Unrelated objects are not machines");
            raider = helper.spawn(CreeperPhantomMod.PHANTOM.get(), 3, 264, 7);
            raider.setNoAi(true);
            Field goalsField = Mob.class.getDeclaredField("goalSelector");
            goalsField.setAccessible(true);
            var goals = (GoalSelector) goalsField.get(raider);
            var machineGoal = find(goals, "BombMachineGoal");
            var inventory = CreeperPhantom.class.getDeclaredMethod("inventoryAt", BlockPos.class);
            inventory.setAccessible(true);
            helper.assertFalse((boolean) inventory.invoke(raider, controllerPos), "Controller must not use container chance");
            helper.assertFalse(machineGoal.canUse(), "Unformed controller must not be attacked");
            controller.formed = true;
            Config.GT_MACHINE_CHANCE.set(0.0);
            raider.tickCount += 300;
            helper.assertFalse(machineGoal.canUse(), "Zero machine probability must suppress raids");
            Config.GT_MACHINE_CHANCE.set(1.0);
            Config.GT_MACHINE_RAIDS.set(false);
            raider.tickCount += 300;
            helper.assertFalse(machineGoal.canUse(), "Integration switch must suppress raids");
            Config.GT_MACHINE_RAIDS.set(true);
            grief.set(false, level.getServer());
            helper.assertFalse(machineGoal.canUse(), "Machine raids must respect mobGriefing");
            grief.set(true, level.getServer());
            raider.tickCount += 300;

            // The controller's own casing blocks its top, but its outdoor side remains approachable.
            level.setBlockAndUpdate(controllerPos.above(), Blocks.STONE.defaultBlockState());
            var player = helper.makeMockSurvivalPlayer();
            var playerPos = helper.absolutePos(new BlockPos(2, 264, 4));
            player.moveTo(playerPos.getX(), playerPos.getY(), playerPos.getZ(), 0, 0);
            helper.assertTrue(raider.canAttack(player), "Fixture player must be visible and attackable");
            raider.setTarget(player);
            goals.tick();
            helper.assertTrue(machineGoal.isRunning(), "Machine raid must preempt player flight at 100 percent chance");
            for (int i = 0; i < 10; i++) machineGoal.tick();
            helper.assertTrue(machineGoal.canContinueToUse(), "Visible player must not immediately cancel the machine raid");
            controller.formed = false;
            helper.assertFalse(machineGoal.canContinueToUse(), "Losing formation must cancel the raid");
            machineGoal.stop();
            controller.formed = true;
            raider.tickCount += 300;
            helper.assertTrue(machineGoal.canUse(), "Restored controller may be attacked again");
            machineGoal.start();

            // The ender variant must use the same GT priority even while angry at a player.
            for (int x = 1; x <= 11; x++) for (int z = 1; z <= 11; z++) helper.setBlock(x, 260, z, Blocks.STONE);
            ender = helper.spawn(CreeperPhantomMod.ENDER_CREEPER.get(), 3, 261, 4);
            ender.setNoAi(true);
            ender.setTarget(player);
            var enderGoals = (GoalSelector) goalsField.get(ender);
            var teleportRaid = find(enderGoals, "TeleportBlockGoal");
            helper.assertTrue(teleportRaid.canUse(), "Ender GT raid must take priority over an angry player target");
            teleportRaid.start();
            helper.assertTrue(ender.isPrimed(), "Ender GT raid must teleport next to the formed controller and ignite");
            ender.discard();
            level.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
            helper.assertFalse(machineGoal.canContinueToUse(), "Removing the controller must cancel the raid");
            machineGoal.stop();
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        } finally {
            if (raider != null) raider.discard();
            if (ender != null) ender.discard();
            level.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(controllerPos.above(), Blocks.AIR.defaultBlockState());
            if (access != null) {
                try { access.set(null, originalAdapter); }
                catch (IllegalAccessException e) { throw new AssertionError(e); }
            }
            grief.set(oldGrief, level.getServer());
            Config.GT_MACHINE_RAIDS.set(oldEnabled);
            Config.GT_MACHINE_CHANCE.set(oldChance);
        }
    }
}
