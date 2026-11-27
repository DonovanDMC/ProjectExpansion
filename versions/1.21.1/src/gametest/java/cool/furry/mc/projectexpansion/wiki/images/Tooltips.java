package cool.furry.mc.projectexpansion.wiki.images;

import cool.furry.mc.projectexpansion.registries.Enchantments;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.wiki.Tooltip;
import cool.furry.mc.projectexpansion.wiki.WikiImage;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

import static cool.furry.mc.projectexpansion.wiki.WikiPlayer.*;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Tooltips {
    private Tooltips() {}

    /********************
     * Alchemical Books *
     ********************/

    @WikiImage("Alchemical-Book/basic-tooltip.png")
    @Tooltip
    public static ItemStack alchemicalBookBasicTooltip() {
        return new ItemStack(Items.BASIC_ALCHEMICAL_BOOK.get());
    }

    @WikiImage("Alchemical-Book/advanced-tooltip.png")
    @Tooltip
    public static ItemStack alchemicalBookAdvancedTooltip() {
        return new ItemStack(Items.ADVANCED_ALCHEMICAL_BOOK.get());
    }

    @WikiImage("Alchemical-Book/master-tooltip.png")
    @Tooltip
    public static ItemStack alchemicalBookMasterTooltip() {
        return new ItemStack(Items.MASTER_ALCHEMICAL_BOOK.get());
    }

    @WikiImage("Alchemical-Book/arcane-tooltip.png")
    @Tooltip
    public static ItemStack alchemicalBookArcaneTooltip() {
        return new ItemStack(Items.ARCANE_ALCHEMICAL_BOOK.get());
    }

    /*************************
     * Alchemical Collection *
     *************************/

    @WikiImage("Alchemical-Collection/tooltip-enabled.png")
    @Tooltip
    public static ItemStack alchemicalCollectionEnabledTooltip() {
        return collectionPickaxe(true);
    }

    @WikiImage("Alchemical-Collection/tooltip-disabled.png")
    @Tooltip
    public static ItemStack alchemicalCollectionDisabledTooltip() {
        return collectionPickaxe(false);
    }

    /***************
     * Compact Sun *
     ***************/

    @WikiImage("Compact-Sun/tooltip.png")
    @Tooltip
    public static ItemStack compactSunTooltip() {
        return new ItemStack(Items.COMPACT_SUN.get());
    }

    /************
     * EMC Lock *
     ************/

    @WikiImage("EMC-Lock/locked-tooltip.png")
    @Tooltip
    public static ItemStack emcLockLockedTooltip() {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG);
        EmcLock.toggle(stack);
        return stack;
    }

    /**************
     * EMC Values *
     **************/

    // a final star's emc, shortened and in full
    @WikiImage("EMC-Values/shortened-tooltip.png")
    @Tooltip
    public static ItemStack shortenedEmcTooltip() {
        return new ItemStack(Items.FINAL_STAR.get());
    }

    @WikiImage(value = "EMC-Values/full-tooltip.png", fullEmc = true)
    @Tooltip
    public static ItemStack fullEmcTooltip() {
        return new ItemStack(Items.FINAL_STAR.get());
    }

    /*******************
     * Learned Tooltip *
     *******************/

    // a diamond chestplate's emc, with a check when it's been learned and a cross when it hasn't
    @WikiImage("Learned-Tooltip/learned.png")
    @Tooltip
    public static ItemStack learnedTooltip(ServerPlayer player) {
        ItemStack chestplate = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
        learn(player, chestplate);
        return chestplate;
    }

    @WikiImage("Learned-Tooltip/not-learned.png")
    @Tooltip
    public static ItemStack notLearnedTooltip(ServerPlayer player) {
        ItemStack chestplate = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
        forget(player, chestplate);
        return chestplate;
    }

    /*********
     * Stars *
     *********/

    @WikiImage("Stars/final-star-tooltip.png")
    @Tooltip
    public static ItemStack finalStarTooltip() {
        return new ItemStack(Items.FINAL_STAR.get());
    }

    @WikiImage("Stars/final-star-shard-tooltip.png")
    @Tooltip
    public static ItemStack finalStarShardTooltip() {
        return new ItemStack(Items.FINAL_STAR_SHARD.get());
    }

    /***********
     * Helpers *
     ***********/

    private static ItemStack collectionPickaxe(boolean active) {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        stack.enchant(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION), 1);
        stack.set(PEDataComponentTypes.ACTIVE.get(), active);
        return stack;
    }
}
