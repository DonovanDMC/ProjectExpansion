package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<ItemFinalStarShard> FINAL_STAR_SHARD = Registry.register("final_star_shard", ItemFinalStarShard::new);
    public static final RegistryObject<ItemFinalStar> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final RegistryObject<ItemMatterUpgrader> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemMatterUpgrader::new);
    public static final RegistryObject<ItemInfiniteFuel> INFINITE_FUEL = Registry.register("infinite_fuel", ItemInfiniteFuel::new);
    public static final RegistryObject<ItemInfiniteSteak> INFINITE_STEAK = Registry.register("infinite_steak", ItemInfiniteSteak::new);
    public static final RegistryObject<BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<BlockItem> COMPACT_SUN = Registry.register("compact_sun", () -> new BlockItem(Blocks.COMPACT_SUN.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<ItemKnowledgeSharingBook> KNOWLEDGE_SHARING_BOOK = Registry.register("knowledge_sharing_book", ItemKnowledgeSharingBook::new);
    public static final RegistryObject<ItemAlchemicalBook> BASIC_ALCHEMICAL_BOOK = Registry.register("basic_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.BASIC));
    public static final RegistryObject<ItemAlchemicalBook> ADVANCED_ALCHEMICAL_BOOK = Registry.register("advanced_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ADVANCED));
    public static final RegistryObject<ItemAlchemicalBook> MASTER_ALCHEMICAL_BOOK = Registry.register("master_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.MASTER));
    public static final RegistryObject<ItemAlchemicalBook> ARCANE_ALCHEMICAL_BOOK = Registry.register("arcane_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ARCANE));
    public static final RegistryObject<BlockItem> CONDENSER_MK3 = Registry.register("condenser_mk3", () -> new BlockItem(Blocks.CONDENSER_MK3.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<ItemArcaneTransmutationTablet> ARCANE_TRANSMUTATION_TABLET = Registry.register("arcane_transmutation_tablet", ItemArcaneTransmutationTablet::new);
    public static final RegistryObject<BlockItem> ARCANE_TRANSMUTATION_TABLE = Registry.register("arcane_transmutation_table", () -> new BlockItem(Blocks.ARCANE_TRANSMUTATION_TABLE.get(), new Item.Properties().rarity(Rarity.RARE).fireResistant()));
}
