package net.creeperphantom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue REPLACEMENT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue PHANTOM_NATURAL_SPAWNS;
    public static final ForgeConfigSpec.BooleanValue ENDER_NATURAL_SPAWNS;
    public static final ForgeConfigSpec.DoubleValue ENDER_SPAWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CONTAINER_CHANCE;
    public static final ForgeConfigSpec.IntValue SEARCH_INTERVAL;
    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue FUSE_TICKS;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ForgeConfigSpec.DoubleValue CHARGED_EXPLOSION_POWER;
    public static final ForgeConfigSpec.BooleanValue MODDED_CONTAINERS;
    public static final ForgeConfigSpec.BooleanValue GT_MACHINE_RAIDS;
    public static final ForgeConfigSpec.DoubleValue GT_MACHINE_CHANCE;

    static {
        var b = new ForgeConfigSpec.Builder();
        PHANTOM_NATURAL_SPAWNS = b.comment("Enable natural Creeper Phantom spawns. Does not affect eggs, commands or existing mobs.")
                .define("phantomNaturalSpawns", true);
        REPLACEMENT_CHANCE = b.comment("Phantom spawn multiplier. Kept under the old key for compatibility: 0.25 replaces 25%, 1 replaces all, 2 requests two per valid vanilla spawn. Fractional extras are random; space and other spawn rules still apply.")
                .defineInRange("naturalReplacementChance", 0.25, 0.0, 64.0);
        ENDER_NATURAL_SPAWNS = b.comment("Enable natural Ender Creeper spawns. Does not affect eggs, commands or existing mobs.")
                .define("enderNaturalSpawns", true);
        ENDER_SPAWN_MULTIPLIER = b.comment("Ender Creeper multiplier per valid natural Enderman spawn. Same semantics as naturalReplacementChance.")
                .defineInRange("enderSpawnMultiplier", 0.25, 0.0, 64.0);
        CONTAINER_CHANCE = b.comment("Autonomous container raid chance per search, not per tick. Phantoms prefer visible players; Ender Creepers only prefer provoked combat targets. Staring is never required to raid containers.")
                .defineInRange("containerBombingChance", 0.10, 0.0, 1.0);
        SEARCH_INTERVAL = b.comment("Ticks between block-target search rolls (containers and GT machines; 20 ticks = 1 second).")
                .defineInRange("containerSearchInterval", 200, 20, 12000);
        SEARCH_RADIUS = b.comment("Horizontal container search radius. Only already-loaded chunks are inspected.")
                .defineInRange("containerSearchRadius", 32, 8, 64);
        MODDED_CONTAINERS = b.comment("Include Forge item-handler capabilities as well as vanilla Container inventories.")
                .define("attackModdedContainers", true);
        GT_MACHINE_RAIDS = b.comment("Optionally raid formed GTCEu multiblock controllers, including addon machines. May take priority over players.")
                .define("attackGtMultiblocks", true);
        GT_MACHINE_CHANCE = b.comment("Independent chance per machine search, including while targeting a player. Controllers never use the container chance.")
                .defineInRange("gtMultiblockBombingChance", 0.25, 0.0, 1.0);
        FUSE_TICKS = b.comment("Audible fuse after reaching a player or container. Once lit, the fuse cannot be cancelled by escaping.")
                .defineInRange("fuseTicks", 20, 5, 200);
        EXPLOSION_POWER = b.comment("Vanilla creeper strength is 3. Explosions obey mobGriefing and Forge explosion events.")
                .defineInRange("explosionPower", 3.0, 0.5, 12.0);
        CHARGED_EXPLOSION_POWER = b.comment("Lightning-charged form detonates immediately on attack contact, without a fuse. Vanilla charged creeper strength is 6.")
                .defineInRange("chargedExplosionPower", 6.0, 0.5, 24.0);
        SPEC = b.build();
    }

    private Config() {}
}
