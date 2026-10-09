package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.item.FuelBlockItem;
import cool.furry.mc.neoforge.projectexpansion.item.ItemFuel;
import cool.furry.mc.neoforge.projectexpansion.registries.Blocks;
import cool.furry.mc.neoforge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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


    public static final List<Fuel> COMMON_ITEMS = List.of(ALCHEMICAL, MOBIUS, AETERNALIS);
    public static final List<Fuel> UNCOMMON_ITEMS = List.of(MAGENTA, PURPLE, VIOLET, BLUE);
    public static final List<Fuel> RARE_ITEMS = List.of(CYAN, GREEN, LIME, YELLOW);
    public static final List<Fuel> EPIC_ITEMS = List.of(ORANGE, WHITE);

    public static final Fuel[] VALUES = values();

    public final String name;
    @Nullable
    public final Supplier<Item> existingItem;
    @Nullable
    private DeferredHolder<Item, Item> item = null;
    @Nullable
    private DeferredHolder<Block, Block> block = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> blockItem = null;
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
    public int getBurnTime(@Nullable RecipeType<?> type) {
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
            case ITEM -> item = Items.Registry.register(String.format("%s_fuel", name), () -> new ItemFuel(this));
            case BLOCK -> {
                block = Blocks.Registry.register(String.format("%s_fuel_block", name), () -> new Block(Block.Properties.of().requiresCorrectToolForDrops().strength(0.5F, 1.5F)));
                blockItem = Items.Registry.register(String.format("%s_fuel_block", name), () -> new FuelBlockItem(this));
            }
        }
    }

    public static void registerAll() {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    public static void setAllCreativeTab(CreativeModeTab.Output output) {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.setCreativeTab(output, type)));
    }

    private void setCreativeTab(CreativeModeTab.Output output, RegistrationType type) {
        if (type == RegistrationType.ITEM && item != null) output.accept(item.get());
        if (type == RegistrationType.BLOCK && blockItem != null) output.accept(blockItem.get());
    }

    private enum RegistrationType {
        ITEM,
        BLOCK
    }

    public static @Nullable Fuel matterToFuel(Matter matter) {
        return switch (matter) {
            case BASIC -> ALCHEMICAL;
            case DARK -> MOBIUS;
            case RED -> AETERNALIS;
            default -> {
                for (Fuel fuel : VALUES) {
                    if (fuel.name.equals(matter.name().toLowerCase(Locale.US))) yield fuel;
                }
                yield null;
            }
        };
    }

    public static @Nullable Matter fuelToMatter(Fuel fuel) {
        return switch (fuel) {
            case ALCHEMICAL -> Matter.BASIC;
            case MOBIUS -> Matter.DARK;
            case AETERNALIS -> Matter.RED;
            default -> {
                for (Matter matter : Matter.VALUES) {
                    if (matter.name().toLowerCase(Locale.US).equals(fuel.name)) yield matter;
                }
                yield null;
            }
        };
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
