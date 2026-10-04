package com.example.ironnest;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(IronNestMod.MODID)
public class IronNestMod {
    public static final String MODID = "ironnest";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<Block> ARTILLERY_CANNON = BLOCKS.register("artillery_cannon",
            () -> new ArtilleryCannonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final RegistryObject<Item> ARTILLERY_CANNON_ITEM = ITEMS.register("artillery_cannon",
            () -> new BlockItem(ARTILLERY_CANNON.get(), new Item.Properties()));

    public static final RegistryObject<Item> ARTILLERY_SHELL = ITEMS.register("artillery_shell",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<EntityType<ArtilleryShellEntity>> SHELL = ENTITIES.register("artillery_shell",
            () -> EntityType.Builder.<ArtilleryShellEntity>of(ArtilleryShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2)
                    .build("artillery_shell"));

    public IronNestMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ARTILLERY_CANNON_ITEM);
            event.accept(ARTILLERY_SHELL);
        }
    }
}
