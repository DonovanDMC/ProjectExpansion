package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.util.Star;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> ARCANE_TABLET = Registry.register("arcane_tablet", ItemArcaneTablet::new);
    public static final RegistryObject<Item> ARCANE_TABLE  = Registry.register("arcane_table", () -> new BlockItem(Blocks.ARCANE_TABLE.get(), new Item.Properties().group(Main.group).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PERSONAL_LINK = Registry.register("personal_link", () -> new BlockItem(Blocks.PERSONAL_LINK.get(), new Item.Properties().group(Main.group)));
    public static final RegistryObject<Item> FINAL_STAR_SHARD = Registry.register("final_star_shard", () -> new Item(new Item.Properties().maxStackSize(1).rarity(Rarity.RARE).group(Main.group)));
    public static final RegistryObject<Item> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
}
