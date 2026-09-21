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
        goalSelector.addGoal(0, new BombContainerGoal());
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
        if (blockEntity instanceof Container container) return container.getContainerSize() > 0;
        if (!Config.MODDED_CONTAINERS.get()) return false;
        if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).map(handler -> handler.getSlots() > 0).orElse(false)) return true;
        for (Direction side : Direction.values()) {
            if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).map(handler -> handler.getSlots() > 0).orElse(false)) return true;
        }
        return false;
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

    private final class BombContainerGoal extends Goal {
        private BlockPos container;
        private int nextSearch;
        private int flightTicks;

        BombContainerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isPrimed() || getTarget() != null || tickCount < nextSearch) return false;
            nextSearch = tickCount + Config.SEARCH_INTERVAL.get() + random.nextInt(40);
            if (!ForgeEventFactory.getMobGriefingEvent(level(), CreeperPhantom.this)
                    || acquireVisiblePlayer() || catsNearby()
                    || random.nextDouble() >= Config.CONTAINER_CHANCE.get()) return false;
            container = findContainer();
            return container != null;
        }

        private BlockPos findContainer() {
            if (!(level() instanceof ServerLevel server)) return null;
            int radius = Config.SEARCH_RADIUS.get();
            BlockPos origin = blockPosition();
            BlockPos best = null;
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
                        if (distance < closest && inventoryAt(pos) && exposed(pos)) {
                            closest = distance;
                            best = pos.immutable();
                        }
                    }
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return !isPrimed() && getTarget() == null && container != null && flightTicks < 200
                    && !horizontalCollision && inventoryAt(container) && exposed(container)
                    && ForgeEventFactory.getMobGriefingEvent(level(), CreeperPhantom.this);
        }

        @Override
        public void start() {
            flightTicks = 0;
            bombingPoint = approach(container);
            playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 0.7F);
        }

        @Override
        public void stop() {
            bombingPoint = null;
            container = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            flightTicks++;
            if (flightTicks % 10 == 0 && (acquireVisiblePlayer() || catsNearby())) {
                stop();
                return;
            }
            if (bombingPoint != null && position().distanceToSqr(bombingPoint) < 2.25) ignite();
        }
    }
}
