package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.item.BlockFuelItem;
import cool.furry.mc.forge.projectexpansion.item.ItemFuel;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Supplier;


@SuppressWarnings("unused")
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

    public int getBurnTime() { return getBurnTime(null); }
    public int getBurnTime(@Nullable RecipeType<?> type) {
        return item == null ? -1 : PEItems.AETERNALIS_FUEL.get().getBurnTime(new ItemStack(item.get()), type);
    }

    public @Nullable Item getItem() {
        return item == null ? null : item.get();
    }

    public @Nullable Block getBlock() {
        return block == null ? null : block.get();
    }

    public @Nullable BlockItem getBlockItem() {
        return blockItem == null ? null : blockItem.get();
    }

    private void register(RegistrationType reg) {
        if (!hasItem) return;
        switch (reg) {
            case ITEM -> item = Items.Registry.register(String.format("%s_fuel", name), () -> new ItemFuel(this));
            case BLOCK -> {
                block = Blocks.Registry.register(String.format("%s_fuel_block", name), () -> new Block(Block.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(0.5F, 1.5F)));
                blockItem = Items.Registry.register(String.format("%s_fuel_block", name), () -> new BlockFuelItem(this));
            }
        }
    }

    public static void registerAll() {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    private enum RegistrationType {
        ITEM,
        BLOCK
    }
}
