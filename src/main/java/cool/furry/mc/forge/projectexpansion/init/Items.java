package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.ItemArcaneTablet;
import cool.furry.mc.forge.projectexpansion.item.ItemFinalStar;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> ARCANE_TABLET = Registry.register("arcane_tablet", ItemArcaneTablet::new);
    public static final RegistryObject<BlockItem> ARCANE_TABLE = Registry.register("arcane_table", () -> new BlockItem(Blocks.ARCANE_TABLE.get(), new Item.Properties().group(Main.group).rarity(Rarity.RARE)));
    public static final RegistryObject<BlockItem> EMC_LINK = Registry.register("emc_link", () -> new BlockItem(Blocks.EMC_LINK.get(), new Item.Properties().group(Main.group)));
    public static final RegistryObject<Item> FINAL_STAR_SHARD = Registry.register("final_star_shard", () -> new Item(new Item.Properties().maxStackSize(1).rarity(Rarity.RARE).group(Main.group)));
    public static final RegistryObject<Item> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final RegistryObject<Item> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemUpgrade::new);
    public static final RegistryObject<BlockItem> EMC_EXPORT = Registry.register("emc_export", () -> new BlockItem(Blocks.EMC_EXPORT.get(), new Item.Properties().group(Main.group)));
    public static final RegistryObject<BlockItem> EMC_IMPORT = Registry.register("emc_import", () -> new BlockItem(Blocks.EMC_IMPORT.get(), new Item.Properties().group(Main.group)));
    public static final RegistryObject<BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().group(Main.group).rarity(Rarity.EPIC)));
}
