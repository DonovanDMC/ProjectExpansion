package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
}
