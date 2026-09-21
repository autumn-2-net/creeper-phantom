package net.creeperphantom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue REPLACEMENT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CONTAINER_CHANCE;
    public static final ForgeConfigSpec.IntValue SEARCH_INTERVAL;
    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue FUSE_TICKS;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ForgeConfigSpec.DoubleValue CHARGED_EXPLOSION_POWER;
    public static final ForgeConfigSpec.BooleanValue MODDED_CONTAINERS;

    static {
        var b = new ForgeConfigSpec.Builder();
        REPLACEMENT_CHANCE = b.comment("Fraction of naturally spawned vanilla phantoms replaced. All vanilla insomnia checks still apply.")
                .defineInRange("naturalReplacementChance", 0.25, 0.0, 1.0);
        CONTAINER_CHANCE = b.comment("Chance per search while no visible attackable player can be acquired. Not a per-tick chance.")
                .defineInRange("containerBombingChance", 0.10, 0.0, 1.0);
        SEARCH_INTERVAL = b.comment("Ticks between container search rolls (20 ticks = 1 second).")
                .defineInRange("containerSearchInterval", 200, 20, 12000);
        SEARCH_RADIUS = b.comment("Horizontal container search radius. Only already-loaded chunks are inspected.")
                .defineInRange("containerSearchRadius", 32, 8, 64);
        MODDED_CONTAINERS = b.comment("Include Forge item-handler capabilities as well as vanilla Container inventories.")
                .define("attackModdedContainers", true);
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
