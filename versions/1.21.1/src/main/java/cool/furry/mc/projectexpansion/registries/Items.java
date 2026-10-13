package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.item.*;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(Registries.ITEM, Main.MOD_ID);

    public static final DeferredHolder<Item, ItemFinalStarShard> FINAL_STAR_SHARD = Registry.register("final_star_shard", ItemFinalStarShard::new);
    public static final DeferredHolder<Item, ItemFinalStar> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final DeferredHolder<Item, ItemMatterUpgrader> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemMatterUpgrader::new);
    public static final DeferredHolder<Item, ItemInfiniteFuel> INFINITE_FUEL = Registry.register("infinite_fuel", ItemInfiniteFuel::new);
    public static final DeferredHolder<Item, ItemInfiniteSteak> INFINITE_STEAK = Registry.register("infinite_steak", ItemInfiniteSteak::new);
    public static final DeferredHolder<Item, BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> COMPACT_SUN = Registry.register("compact_sun", () -> new BlockItem(Blocks.COMPACT_SUN.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, ItemKnowledgeSharingBook> KNOWLEDGE_SHARING_BOOK = Registry.register("knowledge_sharing_book", ItemKnowledgeSharingBook::new);
    public static final DeferredHolder<Item, ItemAlchemicalBook> BASIC_ALCHEMICAL_BOOK = Registry.register("basic_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.BASIC));
    public static final DeferredHolder<Item, ItemAlchemicalBook> ADVANCED_ALCHEMICAL_BOOK = Registry.register("advanced_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ADVANCED));
    public static final DeferredHolder<Item, ItemAlchemicalBook> MASTER_ALCHEMICAL_BOOK = Registry.register("master_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.MASTER));
    public static final DeferredHolder<Item, ItemAlchemicalBook> ARCANE_ALCHEMICAL_BOOK = Registry.register("arcane_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ARCANE));
    public static final DeferredHolder<Item, BlockItem> CONDENSER_MK3 = Registry.register("condenser_mk3", () -> new BlockItem(Blocks.CONDENSER_MK3.get(), new Item.Properties().fireResistant()));
    public static final DeferredHolder<Item, ItemArcaneTransmutationTablet> ARCANE_TRANSMUTATION_TABLET = Registry.register("arcane_transmutation_tablet", ItemArcaneTransmutationTablet::new);
    public static final DeferredHolder<Item, BlockItem> ARCANE_TRANSMUTATION_TABLE = Registry.register("arcane_transmutation_table", () -> new BlockItem(Blocks.ARCANE_TRANSMUTATION_TABLE.get(), new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> IANTHIUM_ORE = Registry.register("ianthium_ore", () -> new BlockItem(Blocks.IANTHIUM_ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DEEPSLATE_IANTHIUM_ORE = Registry.register("deepslate_ianthium_ore", () -> new BlockItem(Blocks.DEEPSLATE_IANTHIUM_ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> RAW_IANTHIUM_BLOCK = Registry.register("raw_ianthium_block", () -> new BlockItem(Blocks.RAW_IANTHIUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> IANTHIUM_BLOCK = Registry.register("ianthium_block", () -> new BlockItem(Blocks.IANTHIUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_IANTHIUM = Registry.register("raw_ianthium", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IANTHIUM_INGOT = Registry.register("ianthium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IANTHIUM_NUGGET = Registry.register("ianthium_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> EMC_REFINERY = Registry.register("emc_refinery", () -> new BlockItem(Blocks.EMC_REFINERY.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> EMC_PIPE = Registry.register("emc_pipe", () -> new BlockItem(Blocks.EMC_PIPE.get(), new Item.Properties()));
    public static final Map<EmcStorageTier, DeferredHolder<Item, BlockItem>> EMC_STORAGE = Util.make(new EnumMap<>(EmcStorageTier.class), map -> {
        for (EmcStorageTier tier : EmcStorageTier.VALUES) map.put(tier, Registry.register(tier.getSerializedName() + "_emc_storage", () -> new BlockItem(Blocks.EMC_STORAGE.get(tier).get(), new Item.Properties().stacksTo(1))));
    });
    public static final DeferredHolder<Item, ItemWrench> WRENCH = Registry.register("wrench", ItemWrench::new);
    public static final DeferredHolder<Item, ItemPipeUpgrade> BASIC_PIPE_UPGRADE = Registry.register("basic_pipe_upgrade", () -> new ItemPipeUpgrade(PipeUpgrade.BASIC));
    public static final DeferredHolder<Item, ItemPipeUpgrade> IMPROVED_PIPE_UPGRADE = Registry.register("improved_pipe_upgrade", () -> new ItemPipeUpgrade(PipeUpgrade.IMPROVED));
    public static final DeferredHolder<Item, ItemPipeUpgrade> ADVANCED_PIPE_UPGRADE = Registry.register("advanced_pipe_upgrade", () -> new ItemPipeUpgrade(PipeUpgrade.ADVANCED));
    public static final DeferredHolder<Item, ItemPipeUpgrade> ULTIMATE_PIPE_UPGRADE = Registry.register("ultimate_pipe_upgrade", () -> new ItemPipeUpgrade(PipeUpgrade.ULTIMATE));
    public static final DeferredHolder<Item, ItemPipeUpgrade> INFINITY_PIPE_UPGRADE = Registry.register("infinity_pipe_upgrade", () -> new ItemPipeUpgrade(PipeUpgrade.INFINITY));
}
