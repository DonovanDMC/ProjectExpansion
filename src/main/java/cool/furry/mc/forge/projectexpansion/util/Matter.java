package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.Blocks;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemCompressedEnergyCollector;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("restriction")
public enum Matter {
    BASIC("basic", false, 1, () -> net.minecraft.item.Items.DIAMOND_BLOCK),
    DARK("dark", false, 2, PEItems.DARK_MATTER),
    RED("red", false, 3, PEItems.RED_MATTER),
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
    WHITE("white", true, 14, null),
    FADING("fading", true, 15, null),
    FINAL("final", false, 16, null);

    public static final Matter[] VALUES = values();

    public final String name;
    public final boolean hasItem;
    public final int level;
    public final long collectorOutput;
    public final long relayBonus;
    public final long relayTransfer;
    @Nullable
    public final Supplier<Item> existingItem;
    public final Rarity rarity;
    @Nullable
    private RegistryObject<Item> itemMatter = null;
    @Nullable
    private RegistryObject<Block> powerFlower = null;
    @Nullable
    private RegistryObject<BlockItem> itemPowerFlower = null;
    @Nullable
    private RegistryObject<Block> collector = null;
    @Nullable
    private RegistryObject<BlockItem> itemCollector = null;
    @Nullable
    private RegistryObject<Item> itemCompressedCollector = null;
    @Nullable
    private RegistryObject<Block> relay = null;
    @Nullable
    private RegistryObject<BlockItem> itemRelay = null;
    Matter(String name, boolean hasItem, int level, @Nullable Supplier<Item> existingItem) {
        this.name = name;
        this.hasItem = hasItem;
        this.level = level;
        this.collectorOutput = calcSomeFactorialShitOrSomething(TemporaryValues.COLLECTOR_BASE, level);
        this.relayBonus = calcSomeFactorialShitOrSomething(TemporaryValues.RELAY_BONUS_BASE, level);
        this.relayTransfer = calcSomeFactorialShitOrSomething(TemporaryValues.RELAY_TRANSFER_BASE, level);
        this.existingItem = existingItem;
        this.rarity =
            level >= EPIC_THRESHOLD ? Rarity.EPIC :
                level >= RARE_THRESHOLD ? Rarity.RARE :
                    level >= UNCOMMON_THRESHOLD ? Rarity.UNCOMMON :
                        Rarity.COMMON;
    }

    private Long calcSomeFactorialShitOrSomething(Long base, int level) {
        try {
            long i = base;
            for(int v = 1; v <= level; v++) i = Math.multiplyExact(i, Long.valueOf(v));
            return i;
        } catch (ArithmeticException err) {
            return Long.MAX_VALUE;
        }
    }

    private static final class TemporaryValues {
        static Long COLLECTOR_BASE = 6L;
        static Long RELAY_BONUS_BASE = 1L;
        static Long RELAY_TRANSFER_BASE = 64L;
    }

    public long getPowerFlowerOutput() {
        try {
            return Math.addExact(
                    Math.multiplyExact(
                            collectorOutput, 18L
                    ),
                    Math.multiplyExact(
                            relayBonus, 30L
                    )
            ) * Config.powerflowerMultiplier.get();
        } catch (ArithmeticException err) {
            return Long.MAX_VALUE;
        }
    }

    public long getPowerFlowerOutputForTicks(int ticks) {
        if(ticks == 20) return getPowerFlowerOutput();
        long div20 = getPowerFlowerOutput() / 20;
        return Math.round((double) div20 * ticks);
    }

    /*
    unless we figure out a way to skip ticks or hard code numbers, dynamically changing the
    tick rate of these 3 will grossly duplicate emc
    */

    public long getCollectorOutput() {
        return collectorOutput;
    }

    public long getCollectorOutputForTicks(int ticks) {
        return getCollectorOutput();
    }

    public long getRelayBonus() {
        return relayBonus;
    }

    public long getRelayBounsForTicks(int ticks) {
        return getRelayBonus();
    }

    public long getRelayTransfer() {
        return relayTransfer;
    }
    public long getRelayTransferForTicks(int ticks) {
        return getRelayTransfer();
    }

    public @Nullable Item getMatter() {
        return itemMatter == null ? null : itemMatter.get();
    }

    public @Nullable Block getPowerFlower() {
        return powerFlower == null ? null : powerFlower.get();
    }

    public @Nullable BlockItem getPowerFlowerItem() {
        return itemPowerFlower == null ? null : itemPowerFlower.get();
    }

    public @Nullable Block getRelay() {
        return relay == null ? null : relay.get();
    }

    public @Nullable BlockItem getRelayItem() {
        return itemRelay == null ? null : itemRelay.get();
    }

    public @Nullable Block getCollector() {
        return collector == null ? null : collector.get();
    }

    public @Nullable BlockItem getCollectorItem() {
        return itemCollector == null ? null : itemCollector.get();
    }

    public @Nullable Item getCompressedCollectorItem() {
        return itemCompressedCollector == null ? null : itemCompressedCollector.get();
    }

    public static final int UNCOMMON_THRESHOLD = 4;
    public static final int RARE_THRESHOLD = 15;
    public static final int EPIC_THRESHOLD = 16;

    private void register() {
        if(hasItem) itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().group(Main.group).rarity(this.rarity)));
        powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(this));
        itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(powerFlower).get(), new Item.Properties().group(Main.group).rarity(this.rarity)));
        collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(this));
        itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(collector).get(), new Item.Properties().group(Main.group).rarity(this.rarity)));
        itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedEnergyCollector(this));
        relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(this));
        itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(relay).get(), new Item.Properties().group(Main.group).rarity(this.rarity)));
    }

    public static void registerAll() {
        Arrays.stream(VALUES).forEach(Matter::register);
    }
}
