package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.ItemFinalStar;
import cool.furry.mc.forge.projectexpansion.item.ItemInfiniteFuel;
import cool.furry.mc.forge.projectexpansion.item.ItemInfiniteSteak;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> FINAL_STAR_SHARD = Registry.register("final_star_shard", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab)));
    public static final RegistryObject<Item> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final RegistryObject<Item> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemUpgrade::new);
    public static final RegistryObject<Item> INFINITE_FUEL = Registry.register("infinite_fuel", ItemInfiniteFuel::new);
    public static final RegistryObject<Item> INFINITE_STEAK = Registry.register("infinite_steak", ItemInfiniteSteak::new);
    public static final RegistryObject<BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().tab(Main.tab).rarity(Rarity.EPIC)));
}
