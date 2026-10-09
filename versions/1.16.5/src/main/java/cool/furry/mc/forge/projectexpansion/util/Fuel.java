package cool.furry.mc.forge.projectexpansion.util;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.forge.projectexpansion.item.FuelBlockItem;
import cool.furry.mc.forge.projectexpansion.item.ItemFuel;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


@SuppressWarnings("unused")
public enum Fuel {
    ALCHEMICAL(PEItems.ALCHEMICAL_COAL::asItem),
    MOBIUS(PEItems.MOBIUS_FUEL::asItem),
    AETERNALIS(PEItems.AETERNALIS_FUEL::asItem),
    MAGENTA(null),
    PINK(null),
    PURPLE(null),
    VIOLET(null),
    BLUE(null),
    CYAN(null),
    GREEN(null),
    LIME(null),
    YELLOW(null),
    ORANGE(null),
    WHITE(null);


    public static final List<Fuel> COMMON_ITEMS = ImmutableList.of(ALCHEMICAL, MOBIUS, AETERNALIS);
    public static final List<Fuel> UNCOMMON_ITEMS = ImmutableList.of(MAGENTA, PURPLE, VIOLET, BLUE);
    public static final List<Fuel> RARE_ITEMS = ImmutableList.of(CYAN, GREEN, LIME, YELLOW);
    public static final List<Fuel> EPIC_ITEMS = ImmutableList.of(ORANGE, WHITE);

    public static final Fuel[] VALUES = values();

    public final String name;
    @Nullable
    public final Supplier<Item> existingItem;
    @Nullable
    private RegistryObject<Item> item = null;
    @Nullable
    private RegistryObject<Block> block = null;
    @Nullable
    private RegistryObject<BlockItem> blockItem = null;
    Fuel(@Nullable Supplier<Item> existingItem) {
        this.name = name().toLowerCase(Locale.US);
        this.existingItem = existingItem;
    }

    public Rarity getRarity() {
        if (COMMON_ITEMS.contains(this)) return Rarity.COMMON;
        if (UNCOMMON_ITEMS.contains(this)) return Rarity.UNCOMMON;
        if (RARE_ITEMS.contains(this)) return Rarity.RARE;
        if (EPIC_ITEMS.contains(this)) return Rarity.EPIC;
        return Rarity.COMMON;
    }

    public int getBurnTime() { return getBurnTime(null); }
    public int getBurnTime(@Nullable IRecipeType<?> type) {
        return item == null ? -1 : new ItemStack(PEItems.AETERNALIS_FUEL.get()).getBurnTime(type);
    }

    public long getCollectorEMCLimit() {
        return getCollectorEMCLimit(Objects.requireNonNull(fuelToMatter(this)));
    }

    public @Nullable Item getItem() {
        return item == null ? null : item.get();
    }
    public @Nullable Item getItemOrExisting() {
        return item == null ? existingItem == null ? null : existingItem.get() : item.get();
    }

    public @Nullable Block getBlock() {
        return block == null ? null : block.get();
    }

    public @Nullable BlockItem getBlockItem() {
        return blockItem == null ? null : blockItem.get();
    }

    private void register(RegistrationType reg) {
        if (this.existingItem != null) return;
        switch (reg) {
            case ITEM: item = Items.Registry.register(String.format("%s_fuel", name), () -> new ItemFuel(this)); break;
            case BLOCK: {
                    block = Blocks.Registry.register(String.format("%s_fuel_block", name), () -> new Block(Block.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(0.5F, 1.5F)));
                    blockItem = Items.Registry.register(String.format("%s_fuel_block", name), () -> new FuelBlockItem(this));
                } break;
        }
    }

    public static void registerAll() {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    public static void setAllCreativeTab(Consumer<IItemProvider> output) {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.setCreativeTab(output, type)));
    }

    private void setCreativeTab(Consumer<IItemProvider> output, RegistrationType type) {
        if (type == RegistrationType.ITEM && item != null) output.accept(item.get());
        if (type == RegistrationType.BLOCK && blockItem != null) output.accept(blockItem.get());
    }

    private enum RegistrationType {
        ITEM,
        BLOCK
    }

    public static @Nullable Fuel matterToFuel(Matter matter) {
        switch (matter) {
            case BASIC: return ALCHEMICAL;
            case DARK: return MOBIUS;
            case RED: return AETERNALIS;
            default: {
                    for (Fuel fuel : VALUES) {
                        if (fuel.name.equals(matter.name().toLowerCase(Locale.US))) return fuel;
                    }
                    return null;
                }
        }
    }

    public static @Nullable Matter fuelToMatter(Fuel fuel) {
        switch (fuel) {
            case ALCHEMICAL: return Matter.BASIC;
            case MOBIUS: return Matter.DARK;
            case AETERNALIS: return Matter.RED;
            default: {
                    for (Matter matter : Matter.VALUES) {
                        if (matter.name().toLowerCase(Locale.US).equals(fuel.name)) return matter;
                    }
                    return null;
                }
        }
    }

    public static long getCollectorEMCLimit(Matter matter) {
        // the minimum storage needed for a minute straight of maximum output
        BigInteger minute = matter.getCollectorOutput().multiply(BigInteger.valueOf(60));
        // round to nearest multiple of 250, with respect to the order of magnitude
        return Util.safeLongValue(roundToNearest(minute));
    }

    public static BigInteger roundToNearest(BigInteger number) {
        BigInteger orderOfMagnitude = BigInteger.TEN.pow(number.toString().length() - 1);

        // Determine the nearest multiple of 250 based on the order of magnitude
        BigInteger nearest250 = orderOfMagnitude.divide(BigInteger.valueOf(4));

        // Calculate the rounded value (using divideAndRemainder() to determine rounding direction)
        BigInteger[] divRem;
        try {
            divRem = number.divideAndRemainder(nearest250);
        } catch (ArithmeticException ignored) {
            divRem = new BigInteger[]{BigInteger.ONE, BigInteger.ZERO};
        }
        BigInteger roundedValue = divRem[1].compareTo(BigInteger.ZERO) > 0 ? divRem[0].add(BigInteger.ONE) : divRem[0];

        // Multiply the rounded value by the nearest multiple of 250
        return roundedValue.multiply(nearest250);
    }
}
