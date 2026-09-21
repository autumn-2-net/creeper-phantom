package net.creeperphantom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CreeperPhantomMod.ID)
public final class CreeperPhantomMod {
    public static final String ID = "creeperphantom";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final RegistryObject<EntityType<CreeperPhantom>> PHANTOM = ENTITIES.register("creeper_phantom",
            () -> EntityType.Builder.of(CreeperPhantom::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F).clientTrackingRange(10).updateInterval(3)
                    .build(ID + ":creeper_phantom"));
    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register("creeper_phantom_spawn_egg",
            () -> new ForgeSpawnEggItem(PHANTOM, 0x438C35, 0x354B85, new Item.Properties()));
    public static final RegistryObject<EntityType<CreeperPhantom>> CHARGED_PHANTOM = ENTITIES.register("charged_creeper_phantom",
            () -> EntityType.Builder.of(CreeperPhantom::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F).clientTrackingRange(10).updateInterval(3)
                    .build(ID + ":charged_creeper_phantom"));
    public static final RegistryObject<Item> CHARGED_SPAWN_EGG = ITEMS.register("charged_creeper_phantom_spawn_egg",
            () -> new ForgeSpawnEggItem(CHARGED_PHANTOM, 0x438C35, 0x62ECFF, new Item.Properties()));

    public CreeperPhantomMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::creativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(PHANTOM.get(), Monster.createMonsterAttributes().build());
        event.put(CHARGED_PHANTOM.get(), Monster.createMonsterAttributes().build());
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(SPAWN_EGG);
            event.accept(CHARGED_SPAWN_EGG);
        }
    }
}
