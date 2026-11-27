package cool.furry.mc.projectexpansion.wiki.images;

import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Enchantments;
import cool.furry.mc.projectexpansion.util.Star;
import cool.furry.mc.projectexpansion.wiki.ItemGrid;
import cool.furry.mc.projectexpansion.wiki.ItemRow;
import cool.furry.mc.projectexpansion.wiki.WikiImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Items {
    private Items() {}

    /*************************
     * Alchemical Collection *
     *************************/

    // the enchantment on a pickaxe, next to the book it comes from
    @WikiImage("Alchemical-Collection/enchanted-pickaxe.png")
    @ItemRow(scale = 3)
    public static List<ItemStack> alchemicalCollectionEnchantedPickaxe() {
        Holder<Enchantment> collection = Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION);
        ItemStack pickaxe = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        pickaxe.enchant(collection, 1);
        return List.of(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(collection, 1)), pickaxe);
    }

    /************
     * Ianthium *
     ************/

    @WikiImage("Ianthium/items.png")
    @ItemRow(scale = 3)
    public static List<ItemStack> ianthiumItems() {
        return List.of(
                new ItemStack(Blocks.IANTHIUM_ORE.get()), new ItemStack(Blocks.DEEPSLATE_IANTHIUM_ORE.get()), new ItemStack(cool.furry.mc.projectexpansion.registries.Items.RAW_IANTHIUM.get()),
                new ItemStack(Blocks.RAW_IANTHIUM_BLOCK.get()), new ItemStack(cool.furry.mc.projectexpansion.registries.Items.IANTHIUM_INGOT.get()), new ItemStack(cool.furry.mc.projectexpansion.registries.Items.IANTHIUM_NUGGET.get()),
                new ItemStack(Blocks.IANTHIUM_BLOCK.get()));
    }

    /*********
     * Stars *
     *********/

    // a row for each size
    @WikiImage("Stars/all-stars.png")
    @ItemGrid
    public static List<List<ItemStack>> allStars() {
        return List.of(stars(Star::asMagnumItem), stars(Star::asColossalItem), stars(Star::asGargantuanItem));
    }

    /***********
     * Helpers *
     ***********/

    private static List<ItemStack> stars(Function<Star, Item> size) {
        return Arrays.stream(Star.VALUES).map(star -> new ItemStack(Objects.requireNonNull(size.apply(star)))).toList();
    }
}
