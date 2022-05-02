package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.block.BlockFuelItem;
import cool.furry.mc.forge.projectexpansion.init.Blocks;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemFuel;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Supplier;


// I couldn't be bothered to come up with some 12 random names so I
// took the lazy way out
public enum Fuel {
    ALCHEMICAL("alchemical", false, 1, PEItems.ALCHEMICAL_COAL::asItem),
    MOBIUS("mobius", false, 2, PEItems.MOBIUS_FUEL::asItem),
    AETERNALIS("aeternalis", false, 3, PEItems.AETERNALIS_FUEL::asItem),
    MAGENTA("magenta", true, 4, null),
    PINK("pink", true, 5, null),
    PURPLE("purple", true, 6, null),
    VIOLET("violet", true, 7, null),
    BLUE("blue", true, 8, null),
    CYAN("cyan", true, 9, null),
    GREEN("green", true, 10, null),
    LIME("lime", true, 11, null),
    YELLOW("yellow", true, 12, null),
    ORANGE("orange", true, 13, null),
    WHITE("white", true, 14, null);


    public static final int UNCOMMON_THRESHOLD = 4;
    public static final int RARE_THRESHOLD = 15;
    public static final int EPIC_THRESHOLD = 16;

    public static final Fuel[] VALUES = values();
    public final String name;
    public final boolean hasItem;
    public final int level;
    @Nullable
    public final Supplier<Item> existingItem;
    public final Rarity rarity;
    @Nullable
    private RegistryObject<Item> item = null;
    @Nullable
    private RegistryObject<Block> block = null;
    @Nullable
    private RegistryObject<BlockItem> blockItem = null;

    Fuel(String name, boolean hasItem, int level, @Nullable Supplier<Item> existingItem) {
        this.name = name;
        this.hasItem = hasItem;
        this.level = level;
        this.existingItem = existingItem;
        this.rarity =
            level >= EPIC_THRESHOLD ? Rarity.EPIC :
                level >= RARE_THRESHOLD ? Rarity.RARE :
                    level >= UNCOMMON_THRESHOLD ? Rarity.UNCOMMON :
                        Rarity.COMMON;
    }

    public int getBurnTime() {
        return item == null ? -1 : PEItems.AETERNALIS_FUEL.get().getBurnTime(new ItemStack(item.get()));
    }

    public @Nullable Item getItem() {
        return item == null ? null : item.get();
    }

    public @Nullable Block getBlock() {
        return block == null ? null : block.get();
    }

    @SuppressWarnings("unused")
    public @Nullable BlockItem getBlockItem() {
        return blockItem == null ? null : blockItem.get();
    }

    private void register(RegistrationType reg) {
        if (!hasItem) return;
        switch (reg) {
            case ITEM: {
                item = Items.Registry.register(String.format("%s_fuel", name), () -> new ItemFuel(this));
                break;
            }

            case BLOCK: {
                block = Blocks.Registry.register(String.format("%s_fuel_block", name), () -> new Block(AbstractBlock.Properties.create(Material.ROCK).setRequiresTool().harvestTool(ToolType.PICKAXE).hardnessAndResistance(0.5F, 1.5F)));
                blockItem = Items.Registry.register(String.format("%s_fuel_block", name), () -> new BlockFuelItem(this));
                break;
            }
        }
    }

    public static void registerAll() {
        Arrays.stream(Fuel.RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    private enum RegistrationType {
        ITEM,
        BLOCK
    }
}
