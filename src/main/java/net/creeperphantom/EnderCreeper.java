package net.creeperphantom;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.ForgeEventFactory;

public final class EnderCreeper extends EnderMan implements PowerableMob {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(EnderCreeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(EnderCreeper.class, EntityDataSerializers.BOOLEAN);
    private boolean exploded;

    public EnderCreeper(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        entityData.set(POWERED, type == CreeperPhantomMod.CHARGED_ENDER_CREEPER.get());
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FUSE, -1);
        entityData.define(POWERED, false);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-2, new FuseGoal());
        goalSelector.addGoal(-1, new TeleportBlockGoal());
        goalSelector.addGoal(0, new TeleportAttackGoal());
        // The vanilla target selector is retained: staring, retaliation, pumpkins and anger remain vanilla.
    }

    @Override public boolean isPowered() { return entityData.get(POWERED); }
    public boolean isPrimed() { return entityData.get(FUSE) >= 0; }
    public int fuseRemaining() { return entityData.get(FUSE); }

    @Override public void thunderHit(ServerLevel level, LightningBolt bolt) {
        super.thunderHit(level, bolt);
        entityData.set(POWERED, true);
        if (isPrimed()) detonate();
    }

    private void ignite() {
        if (level().isClientSide || !isAlive() || isPrimed() || exploded) return;
        getNavigation().stop();
        if (isPowered()) detonate();
        else {
            entityData.set(FUSE, Config.FUSE_TICKS.get());
            playSound(SoundEvents.CREEPER_PRIMED, 1, 0.8F);
        }
    }

    private void detonate() {
        if (level().isClientSide || !isAlive() || exploded) return;
        exploded = true;
        dead = true;
        float power = (isPowered() ? Config.CHARGED_EXPLOSION_POWER : Config.EXPLOSION_POWER).get().floatValue();
        level().explode(this, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.MOB);
        discard();
    }

    @Override public boolean doHurtTarget(Entity entity) {
        if (entity instanceof LivingEntity living && validTarget(living)) {
            ignite();
            return true;
        }
        return false;
    }

    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive() && canAttack(target)
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    @Override protected boolean teleport() { return !isPrimed() && super.teleport(); }

    @Override protected void customServerAiStep() {
        if (!isPrimed()) super.customServerAiStep();
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || !isPrimed() || exploded) return;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        int remaining = Math.max(0, fuseRemaining() - 1);
        entityData.set(FUSE, remaining);
        if (remaining == 0 || isPowered()) detonate();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("powered", isPowered());
        tag.putInt("EnderCreeperFuse", fuseRemaining());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("powered")) entityData.set(POWERED, tag.getBoolean("powered"));
        if (tag.contains("EnderCreeperFuse")) entityData.set(FUSE, Mth.clamp(tag.getInt("EnderCreeperFuse"), -1, 200));
    }

    private boolean areaLoaded(AABB box) {
        return box.minY >= level().getMinBuildHeight() && box.maxY < level().getMaxBuildHeight()
                && level().hasChunksAt(Mth.floor(box.minX), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxZ));
    }

    private boolean safeLanding(Vec3 point) {
        double half = getBbWidth() / 2;
        AABB box = new AABB(point.x - half, point.y, point.z - half, point.x + half, point.y + getBbHeight(), point.z + half);
        if (!areaLoaded(box)) return false;
        BlockPos floor = BlockPos.containing(point).below();
        return level().getBlockState(floor).isFaceSturdy(level(), floor, Direction.UP)
                && level().noCollision(this, box) && !level().containsAnyLiquid(box);
    }

    private boolean clearLine(Vec3 from, Vec3 to, BlockPos allowedHit) {
        AABB region = new AABB(from, to).inflate(0.01);
        if (!areaLoaded(region)) return false;
        var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || (allowedHit != null && hit.getBlockPos().equals(allowedHit));
    }

    /** Find a floor and enough headroom without loading chunks, entering liquids, or landing inside the target. */
    private Vec3 findLanding(Vec3 target, BlockPos blockTarget) {
        double phase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 16; i++) {
            double angle = phase + i * Math.PI / 4;
            double radius = i < 8 ? 1.8 : 2.6;
            double x = Math.floor(target.x + Math.cos(angle) * radius) + 0.5;
            double z = Math.floor(target.z + Math.sin(angle) * radius) + 0.5;
            for (int dy = 1; dy >= -3; dy--) {
                Vec3 point = new Vec3(x, Math.floor(target.y) + dy, z);
                if (!safeLanding(point)) continue;
                if (blockTarget != null && !level().canSeeSky(BlockPos.containing(point))) continue;
                if (clearLine(point.add(0, 1, 0), target.add(0, blockTarget == null ? 1 : 0, 0), blockTarget)) return point;
            }
        }
        return null;
    }

    private boolean teleportForAttack(Vec3 target, BlockPos blockTarget) {
        Vec3 landing = findLanding(target, blockTarget);
        if (landing == null) return false;
        var event = ForgeEventFactory.onEnderTeleport(this, landing.x, landing.y, landing.z);
        if (event.isCanceled()) return false;
        Vec3 approved = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        // A relocation by another mod must still land safely next to this target before igniting.
        if (!Double.isFinite(approved.x) || !Double.isFinite(approved.y) || !Double.isFinite(approved.z)
                || approved.distanceToSqr(target) > 25 || !safeLanding(approved)
                || !clearLine(approved.add(0, 1, 0), target.add(0, blockTarget == null ? 1 : 0, 0), blockTarget)) return false;
        Vec3 old = position();
        if (!randomTeleport(approved.x, approved.y, approved.z, true)) return false;
        level().gameEvent(GameEvent.TELEPORT, old, GameEvent.Context.of(this));
        if (!isSilent()) {
            level().playSound(null, old.x, old.y, old.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1, 1);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        }
        ignite();
        return true;
    }

    private boolean eligibleBlock(BlockPos pos, boolean machine) {
        if (!level().hasChunkAt(pos)) return false;
        BlockEntity entity = level().getBlockEntity(pos);
        if (entity == null || entity.isRemoved()) return false;
        GtMultiblocks.Kind kind = GtMultiblocks.classify(entity);
        if (machine) return kind == GtMultiblocks.Kind.FORMED_CONTROLLER;
        if (kind != GtMultiblocks.Kind.OTHER) return false;
        if (entity instanceof Container inventory) return inventory.getContainerSize() > 0;
        if (!Config.MODDED_CONTAINERS.get()) return false;
        if (entity.getCapability(ForgeCapabilities.ITEM_HANDLER).map(h -> h.getSlots() > 0).orElse(false)) return true;
        for (Direction side : Direction.values()) {
            if (entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).map(h -> h.getSlots() > 0).orElse(false)) return true;
        }
        return false;
    }

    private BlockPos findBlock(boolean machine) {
        if (!(level() instanceof ServerLevel server)) return null;
        int radius = Config.SEARCH_RADIUS.get(), inspected = 0, landingChecks = 0;
        BlockPos origin = blockPosition(), best = null;
        double nearest = Double.MAX_VALUE;
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (++inspected > 4096) return best;
                    int dx = pos.getX() - origin.getX(), dz = pos.getZ() - origin.getZ();
                    if (dx * dx + dz * dz > radius * radius || Math.abs(pos.getY() - origin.getY()) > 48) continue;
                    double distance = pos.distToCenterSqr(position());
                    if (distance < nearest && eligibleBlock(pos, machine)
                            && (machine || level().canSeeSky(pos.above()))
                            && clearLine(getEyePosition(), Vec3.atCenterOf(pos), pos)) {
                        // Tall mobs may have no safe floor around many inventories. Bound expensive landing probes.
                        if (++landingChecks > 16) return best;
                        if (findLanding(Vec3.atCenterOf(pos), pos) != null) {
                            nearest = distance;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private final class FuseGoal extends Goal {
        FuseGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return isPrimed(); }
        @Override public void tick() { getNavigation().stop(); setDeltaMovement(Vec3.ZERO); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }

    private final class TeleportAttackGoal extends Goal {
        private int nextAttempt;
        TeleportAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return !isPrimed() && tickCount >= nextAttempt && validTarget(getTarget()); }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() {
            nextAttempt = tickCount + 40;
            LivingEntity target = getTarget();
            if (validTarget(target) && distanceToSqr(target) <= 64 * 64) teleportForAttack(target.position(), null);
        }
    }

    private final class TeleportBlockGoal extends Goal {
        private int nextSearch;
        private BlockPos blockTarget;
        private boolean machine;
        TeleportBlockGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            if (isPrimed() || tickCount < nextSearch) return false;
            nextSearch = tickCount + Config.SEARCH_INTERVAL.get() + random.nextInt(40);
            if (!ForgeEventFactory.getMobGriefingEvent(level(), EnderCreeper.this)) return false;
            machine = true;
            blockTarget = null;
            if (Config.GT_MACHINE_RAIDS.get() && GtMultiblocks.available() && random.nextDouble() < Config.GT_MACHINE_CHANCE.get()) {
                blockTarget = findBlock(true);
            }
            // Neutral nearby players do not suppress autonomous inventory raids.
            if (blockTarget == null && !validTarget(getTarget()) && random.nextDouble() < Config.CONTAINER_CHANCE.get()) {
                machine = false;
                blockTarget = findBlock(false);
            }
            return blockTarget != null;
        }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() {
            if (blockTarget != null && eligibleBlock(blockTarget, machine)
                    && ForgeEventFactory.getMobGriefingEvent(level(), EnderCreeper.this)) teleportForAttack(Vec3.atCenterOf(blockTarget), blockTarget);
        }
    }
}
