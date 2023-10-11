package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> FINAL_STAR_SHARD = Registry.register("final_star_shard", ItemFinalStarShard::new);
    public static final RegistryObject<Item> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final RegistryObject<Item> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemMatterUpgrader::new);
    public static final RegistryObject<Item> INFINITE_FUEL = Registry.register("infinite_fuel", ItemInfiniteFuel::new);
    public static final RegistryObject<Item> INFINITE_STEAK = Registry.register("infinite_steak", ItemInfiniteSteak::new);
    public static final RegistryObject<BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().tab(Main.tab).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> KNOWLEDGE_SHARING_BOOK = Registry.register("knowledge_sharing_book", ItemKnowledgeSharingBook::new);
    public static final RegistryObject<Item> BASIC_ALCHEMICAL_BOOK = Registry.register("basic_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.BASIC));
    public static final RegistryObject<Item> ADVANCED_ALCHEMICAL_BOOK = Registry.register("advanced_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ADVANCED));
    public static final RegistryObject<Item> MASTER_ALCHEMICAL_BOOK = Registry.register("master_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.MASTER));
    public static final RegistryObject<Item> ARCANE_ALCHEMICAL_BOOK = Registry.register("arcane_alchemical_book", () -> new ItemAlchemicalBook(ItemAlchemicalBook.Tier.ARCANE));
}
