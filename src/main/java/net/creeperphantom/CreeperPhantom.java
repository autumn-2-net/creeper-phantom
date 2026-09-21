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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.ForgeEventFactory;

public final class CreeperPhantom extends Phantom implements PowerableMob {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(CreeperPhantom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(CreeperPhantom.class, EntityDataSerializers.BOOLEAN);
    private Vec3 bombingPoint;
    private int lostSightTicks;
    private boolean exploded;

    public CreeperPhantom(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        entityData.set(POWERED, type == CreeperPhantomMod.CHARGED_PHANTOM.get());
        MoveControl vanillaFlight = moveControl;
        moveControl = new MoveControl(this) {
            @Override
            public void tick() {
                if (isPrimed()) {
                    setDeltaMovement(getDeltaMovement().scale(0.75));
                } else if (bombingPoint != null) {
                    Vec3 delta = bombingPoint.subtract(position());
                    double distance = delta.length();
                    if (distance < 0.1) return;
                    Vec3 desired = delta.scale(Math.min(0.85, distance * 0.12) / distance);
                    setDeltaMovement(getDeltaMovement().lerp(desired, 0.18));
                    float yaw = (float) (Mth.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90;
                    setYRot(Mth.rotLerp(0.18F, getYRot(), yaw));
                    yBodyRot = getYRot();
                    yHeadRot = getYRot();
                    setXRot((float) -(Mth.atan2(delta.y, delta.horizontalDistance()) * 180.0 / Math.PI));
                } else {
                    vanillaFlight.tick();
                }
            }
        };
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new BombMachineGoal());
        goalSelector.addGoal(1, new BombContainerGoal());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FUSE, -1);
        entityData.define(POWERED, false);
    }

    @Override
    public boolean isPowered() {
        return entityData.get(POWERED);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        super.thunderHit(level, lightning);
        entityData.set(POWERED, true);
        if (isPrimed()) detonate();
    }

    public boolean isPrimed() {
        return entityData.get(FUSE) >= 0;
    }

    public int fuseRemaining() {
        return entityData.get(FUSE);
    }

    private void ignite() {
        if (level().isClientSide || isPrimed() || !isAlive()) return;
        if (isPowered()) {
            detonate();
            return;
        }
        entityData.set(FUSE, Config.FUSE_TICKS.get());
        playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.65F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // The vanilla phantom swoop calls this on contact. Replace its melee hit with a fuse.
        if (target instanceof LivingEntity living && canAttack(living)) {
            ignite();
            return true;
        }
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && (!(target instanceof Player player)
                || (!player.isCreative() && !player.isSpectator() && getSensing().hasLineOfSight(player)));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || !canAttack(target))) {
            if (++lostSightTicks >= 40 || !target.isAlive()) {
                setTarget(null);
                lostSightTicks = 0;
            }
        } else {
            lostSightTicks = 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || !isPrimed() || exploded) return;
        int remaining = fuseRemaining() - 1;
        entityData.set(FUSE, Math.max(0, remaining));
        if (remaining <= 0 || isPowered()) detonate();
    }

    private void detonate() {
        if (level().isClientSide || exploded || !isAlive()) return;
        exploded = true;
        dead = true;
        float power = (isPowered() ? Config.CHARGED_EXPLOSION_POWER : Config.EXPLOSION_POWER).get().floatValue();
        level().explode(this, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.MOB);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CreeperPhantomFuse", fuseRemaining());
        tag.putBoolean("powered", isPowered());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CreeperPhantomFuse")) entityData.set(FUSE, Mth.clamp(tag.getInt("CreeperPhantomFuse"), -1, 200));
        if (tag.contains("powered")) entityData.set(POWERED, tag.getBoolean("powered"));
    }

    private boolean inventoryAt(BlockPos pos) {
        if (!level().hasChunkAt(pos)) return false;
        BlockEntity blockEntity = level().getBlockEntity(pos);
        if (blockEntity == null || blockEntity.isRemoved()) return false;
        // A controller may expose item slots too. Never give it a second, container-based raid roll.
        if (GtMultiblocks.classify(blockEntity) != GtMultiblocks.Kind.OTHER) return false;
        if (blockEntity instanceof Container container) return container.getContainerSize() > 0;
        if (!Config.MODDED_CONTAINERS.get()) return false;
        if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).map(handler -> handler.getSlots() > 0).orElse(false)) return true;
        for (Direction side : Direction.values()) {
            if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).map(handler -> handler.getSlots() > 0).orElse(false)) return true;
        }
        return false;
    }

    private boolean formedControllerAt(BlockPos pos) {
        if (!level().hasChunkAt(pos)) return false;
        BlockEntity entity = level().getBlockEntity(pos);
        return entity != null && !entity.isRemoved()
                && GtMultiblocks.classify(entity) == GtMultiblocks.Kind.FORMED_CONTROLLER;
    }

    private Vec3 approach(BlockPos pos) {
        return Vec3.atCenterOf(pos).add(0, 0.8, 0);
    }

    private boolean exposed(BlockPos pos) {
        if (!level().hasChunkAt(pos) || !level().canSeeSky(pos.above())) return false;
        // Ray tracing must not pull unloaded chunks into memory while looking for a target.
        BlockPos origin = blockPosition();
        if (!level().hasChunksAt(Math.min(origin.getX(), pos.getX()), Math.min(origin.getZ(), pos.getZ()),
                Math.max(origin.getX(), pos.getX()), Math.max(origin.getZ(), pos.getZ()))) return false;
        BlockHitResult hit = level().clip(new ClipContext(getEyePosition(), approach(pos),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private Vec3 machineApproach(BlockPos controller) {
        Vec3 best = null;
        double nearest = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            double extent = face.getAxis() == Direction.Axis.Y ? getBbHeight() / 2 : getBbWidth() / 2;
            Vec3 point = Vec3.atCenterOf(controller).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.6 + extent))
                    .subtract(0, getBbHeight() / 2, 0);
            double distance = position().distanceToSqr(point);
            if (distance < nearest && machineApproachClear(controller, point)) {
                best = point;
                nearest = distance;
            }
        }
        return best;
    }

    private boolean machineApproachClear(BlockPos controller, Vec3 point) {
        double halfWidth = getBbWidth() / 2;
        AABB arrival = new AABB(point.x - halfWidth, point.y, point.z - halfWidth,
                point.x + halfWidth, point.y + getBbHeight(), point.z + halfWidth);
        BlockPos origin = blockPosition();
        // Check the entire ray and arrival box before any operation that may read world blocks.
        int minX = Mth.floor(Math.min(Math.min(arrival.minX, origin.getX()), controller.getX()));
        int minZ = Mth.floor(Math.min(Math.min(arrival.minZ, origin.getZ()), controller.getZ()));
        int maxX = Mth.floor(Math.max(Math.max(arrival.maxX, origin.getX()), controller.getX()));
        int maxZ = Mth.floor(Math.max(Math.max(arrival.maxZ, origin.getZ()), controller.getZ()));
        if (!level().hasChunksAt(minX, minZ, maxX, maxZ)
                || !level().canSeeSky(BlockPos.containing(point)) || !level().noCollision(this, arrival)) return false;
        // The arrival route must be clear, and the controller itself must be visible from there.
        if (level().clip(new ClipContext(getEyePosition(), point.add(0, getBbHeight() / 2, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) return false;
        BlockHitResult face = level().clip(new ClipContext(point.add(0, getBbHeight() / 2, 0), Vec3.atCenterOf(controller),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return face.getType() == HitResult.Type.MISS || face.getBlockPos().equals(controller);
    }

    private boolean catsNearby() {
        return !level().getEntitiesOfClass(Cat.class, getBoundingBox().inflate(16), Cat::isAlive).isEmpty();
    }

    private boolean acquireVisiblePlayer() {
        Player nearest = null;
        double nearestDistance = 64 * 64;
        for (Player player : level().players()) {
            double distance = distanceToSqr(player);
            if (distance < nearestDistance && player.isAlive() && canAttack(player)) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        if (nearest != null) setTarget(nearest);
        return nearest != null;
    }

    private abstract class BombBlockGoal extends Goal {
        private BlockPos blockTarget;
        private Vec3 attackPoint;
        private int nextSearch;
        private int flightTicks;

        BombBlockGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        abstract boolean enabled();
        abstract boolean playerTakesPriority();
        abstract double chance();
        abstract boolean accepts(BlockPos pos);
        abstract Vec3 approachFor(BlockPos pos);
        abstract boolean pathStillClear(BlockPos pos, Vec3 point);
        double contactDistanceSquared() { return 2.25; }

        @Override
        public boolean canUse() {
            if (!enabled() || isPrimed() || (playerTakesPriority() && getTarget() != null)
                    || tickCount < nextSearch) return false;
            nextSearch = tickCount + Config.SEARCH_INTERVAL.get() + random.nextInt(40);
            if (!ForgeEventFactory.getMobGriefingEvent(level(), CreeperPhantom.this)
                    || (playerTakesPriority() && acquireVisiblePlayer()) || catsNearby()
                    || random.nextDouble() >= chance()) return false;
            RaidTarget found = findBlockTarget();
            if (found == null) return false;
            blockTarget = found.pos();
            attackPoint = found.approach();
            return true;
        }

        private RaidTarget findBlockTarget() {
            if (!(level() instanceof ServerLevel server)) return null;
            int radius = Config.SEARCH_RADIUS.get();
            BlockPos origin = blockPosition();
            RaidTarget best = null;
            double closest = Double.MAX_VALUE;
            int inspected = 0;
            for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
                for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) {
                    LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                        if (++inspected > 4096) return best;
                        int dx = pos.getX() - origin.getX(), dz = pos.getZ() - origin.getZ();
                        if (dx * dx + dz * dz > radius * radius || Math.abs(pos.getY() - origin.getY()) > 48) continue;
                        double distance = pos.distToCenterSqr(position());
                        if (distance < closest && accepts(pos)) {
                            Vec3 point = approachFor(pos);
                            if (point != null) {
                                closest = distance;
                                best = new RaidTarget(pos.immutable(), point);
                            }
                        }
                    }
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return enabled() && !isPrimed() && (!playerTakesPriority() || getTarget() == null)
                    && blockTarget != null && flightTicks < 200
                    && !horizontalCollision && accepts(blockTarget) && pathStillClear(blockTarget, attackPoint)
                    && ForgeEventFactory.getMobGriefingEvent(level(), CreeperPhantom.this);
        }

        @Override
        public void start() {
            flightTicks = 0;
            bombingPoint = attackPoint;
            playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 0.7F);
        }

        @Override
        public void stop() {
            bombingPoint = null;
            blockTarget = null;
            attackPoint = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            flightTicks++;
            if (flightTicks % 10 == 0 && ((playerTakesPriority() && acquireVisiblePlayer()) || catsNearby())) {
                stop();
                return;
            }
            if (bombingPoint != null && position().distanceToSqr(bombingPoint) < contactDistanceSquared()) ignite();
        }
    }

    private record RaidTarget(BlockPos pos, Vec3 approach) {}

    private final class BombContainerGoal extends BombBlockGoal {
        @Override boolean enabled() { return true; }
        @Override boolean playerTakesPriority() { return true; }
        @Override double chance() { return Config.CONTAINER_CHANCE.get(); }
        @Override boolean accepts(BlockPos pos) { return inventoryAt(pos); }
        @Override Vec3 approachFor(BlockPos pos) { return exposed(pos) ? approach(pos) : null; }
        @Override boolean pathStillClear(BlockPos pos, Vec3 point) { return exposed(pos); }
    }

    private final class BombMachineGoal extends BombBlockGoal {
        @Override boolean enabled() { return Config.GT_MACHINE_RAIDS.get() && GtMultiblocks.available(); }
        @Override boolean playerTakesPriority() { return false; }
        @Override double chance() { return Config.GT_MACHINE_CHANCE.get(); }
        @Override boolean accepts(BlockPos pos) { return formedControllerAt(pos); }
        @Override Vec3 approachFor(BlockPos pos) { return machineApproach(pos); }
        @Override boolean pathStillClear(BlockPos pos, Vec3 point) { return machineApproachClear(pos, point); }
        @Override double contactDistanceSquared() { return 0.36; }
    }
}
