package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.*;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.item.ItemCompressedEnergyCollector;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.blocks.MatterBlock;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.material.MaterialColor;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Matter {
    BASIC(  0,  MaterialColor.COLOR_GRAY, null, null),
    DARK(   2,  PEBlocks.DARK_MATTER.getBlock().defaultMaterialColor(), PEItems.DARK_MATTER, PEBlocks.DARK_MATTER::getBlock),
    RED(    4,  PEBlocks.RED_MATTER.getBlock().defaultMaterialColor(), PEItems.RED_MATTER, PEBlocks.RED_MATTER::getBlock),
    MAGENTA(4,  MaterialColor.COLOR_MAGENTA, null,  null),
    PINK(   5,  MaterialColor.COLOR_PINK,  null,  null),
    PURPLE( 5,  MaterialColor.COLOR_PURPLE,  null,  null),
    VIOLET( 6,  MaterialColor.COLOR_PURPLE,  null,  null),
    BLUE(   6,  MaterialColor.COLOR_BLACK,  null,  null),
    CYAN(   7,  MaterialColor.COLOR_CYAN,  null,  null),
    GREEN(  7,  MaterialColor.COLOR_GREEN,  null,  null),
    LIME(   8,  MaterialColor.COLOR_LIGHT_GREEN,  null,  null),
    YELLOW( 8,  MaterialColor.COLOR_YELLOW,  null,  null),
    ORANGE( 9,  MaterialColor.COLOR_ORANGE,  null,  null),
    WHITE(  9,  null,  null,  null),
    FADING( 10,  MaterialColor.COLOR_BLACK, null, null),
    FINAL(  10,  null, Items.FINAL_STAR_SHARD, null);
    public final BigDecimal BASE_COLLECTOR_OUTPUT = BigDecimal.valueOf(4L);
    public final BigDecimal BASE_RELAY_BONUS = BigDecimal.valueOf(1L);
    public final BigDecimal BASE_RELAY_TRANSFER = BigDecimal.valueOf(64L);

    public static final Matter[] VALUES = values();

    public Matter prev() {
        return VALUES[(ordinal() - 1  + VALUES.length) % VALUES.length];
    }

    public Matter next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static final List<Matter> COMMON_ITEMS = List.of(BASIC, DARK, RED);
    public static final List<Matter> UNCOMMON_ITEMS = List.of(MAGENTA, PURPLE, VIOLET, BLUE);
    public static final List<Matter> RARE_ITEMS = List.of(CYAN, GREEN, LIME, YELLOW);
    public static final List<Matter> EPIC_ITEMS = List.of(ORANGE, WHITE, FADING, FINAL);


    public final String name;
    public final boolean hasItem;
    public final boolean hasBlock;
    public final int level;
    public final BigDecimal collectorOutputBase;
    public final BigDecimal relayBonusBase;
    public final BigDecimal relayTransferBase;
    /** @deprecated Due to how 1.19.2 config values work, this will not be set to 100 when fluid efficiency is disabled. */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public final int fluidEfficiency;
    @Nullable
    public final MaterialColor materialColor;
    @Nullable
    public final Supplier<Item> existingItem;
    @Nullable
    public final Supplier<Block> existingBlock;
    @Nullable
    private RegistryObject<Item> itemMatter = null;
    @Nullable
    private RegistryObject<BlockPowerFlower> powerFlower = null;
    @Nullable
    private RegistryObject<BlockItem> itemPowerFlower = null;
    @Nullable
    private RegistryObject<BlockCollector> collector = null;
    @Nullable
    private RegistryObject<BlockItem> itemCollector = null;
    @Nullable
    private RegistryObject<ItemCompressedEnergyCollector> itemCompressedCollector = null;
    @Nullable
    private RegistryObject<BlockRelay> relay = null;
    @Nullable
    private RegistryObject<BlockItem> itemRelay = null;
    @Nullable
    private RegistryObject<BlockEMCLink> emcLink = null;
    @Nullable
    private RegistryObject<BlockItem> itemEMCLink = null;
    @Nullable
    private RegistryObject<BlockItem> itemMatterBlock = null;
    @Nullable
    private RegistryObject<BlockMatter> blockMatterBlock = null;
    Matter(int fluidEfficiency, @Nullable MaterialColor materialColor, @Nullable Supplier<Item> existingItem, @Nullable Supplier<Block> existingBlock) {
        boolean isFinal = name().equals("FINAL"); // we can't access the FINAL member because we're in the constructor
        this.name = name().toLowerCase(Locale.US);
        this.hasItem = existingItem == null && ordinal() != 0;
        this.hasBlock = existingBlock == null && ordinal() != 0 && ordinal() != 15;
        this.level = ordinal() + 1;
        this.collectorOutputBase = getValue(BASE_COLLECTOR_OUTPUT);
        this.relayBonusBase = getValue(BASE_RELAY_BONUS);
        this.relayTransferBase = isFinal ? BigDecimal.valueOf(Long.MAX_VALUE) : getValue(BASE_RELAY_TRANSFER);
        this.fluidEfficiency = fluidEfficiency;
        this.materialColor = materialColor;
        this.existingItem = existingItem;
        this.existingBlock = existingBlock;
    }

    public Rarity getRarity() {
        if (COMMON_ITEMS.contains(this)) return Rarity.COMMON;
        if (UNCOMMON_ITEMS.contains(this)) return Rarity.UNCOMMON;
        if (RARE_ITEMS.contains(this)) return Rarity.RARE;
        if (EPIC_ITEMS.contains(this)) return Rarity.EPIC;
        return Rarity.COMMON;
    }

    public int getLevel() {
        return level;
    }

    private BigDecimal getValue(BigDecimal base) {
        BigDecimal val = base;
        for(int i = 0; i < ordinal(); i++) {
            val = val.multiply(BigDecimal.valueOf(6));
        }

        return val;
    }

    public int getFluidEfficiencyPercentage() {
        if(!Config.enableFluidEfficiency.get()) return 100;
        AtomicInteger efficiency = new AtomicInteger(fluidEfficiency);
        Arrays.stream(VALUES).filter((m) -> m.level < level).forEach((m) -> efficiency.addAndGet(m.fluidEfficiency));
        return efficiency.get();
    }

    /* Limits */
    public BigInteger getPowerFlowerOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(18)).add(relayBonusBase.multiply(BigDecimal.valueOf(30))).multiply(BigDecimal.valueOf(Config.powerflowerMultiplier.get())).toBigInteger();
    }

    public BigInteger getPowerFlowerOutputForTicks(int ticks) {
        if (ticks == 20) return getPowerFlowerOutput();
        BigInteger div20 = getPowerFlowerOutput().divide(BigInteger.valueOf(20));
        return div20.multiply(BigInteger.valueOf(ticks));
    }

    /*
    unless we figure out a way to skip ticks or hard code numbers, dynamically changing the
    tick rate of these 3 will grossly duplicate emc
    */

    public BigInteger getCollectorOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(Config.collectorMultiplier.get())).toBigInteger();
    }

    public BigInteger getCollectorOutputForTicks(int ticks) {
        return getCollectorOutput();
    }

    public BigInteger getRelayBonus() {
        return relayBonusBase.multiply(BigDecimal.valueOf(Config.relayBonusMultiplier.get())).toBigInteger();
    }

    public BigInteger getRelayBonusForTicks(int ticks) {
        return getRelayBonus();
    }

    public BigInteger getRelayTransfer() {
        return relayTransferBase.multiply(BigDecimal.valueOf(Config.relayTransferMultiplier.get())).toBigInteger();
    }
    public BigInteger getRelayTransferForTicks(int ticks) {
        return getRelayTransfer();
    }

    public int getEMCLinkInventorySize() {
        return level * 3;
    }

    public BigInteger getEMCLinkEMCLimit() {
        return BigDecimal.valueOf(16)
                .pow(level)
                .multiply(BigDecimal.valueOf(Config.emcLinkEMCLimitMultiplier.get())).toBigInteger();
    }

    public int getEMCLinkItemLimit() {
        try {
            return BigDecimal.valueOf(2).pow(level - 1).multiply(BigDecimal.valueOf(Config.emcLinkItemLimitMultiplier.get())).intValueExact();
        } catch(ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public int getEMCLinkFluidLimit() {
        try {
            return BigDecimal.valueOf(2).pow(level - 1).multiply(BigDecimal.valueOf(1000)).multiply(BigDecimal.valueOf(Config.emcLinkFluidLimitMultiplier.get())).intValueExact();
        } catch(ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public MutableComponent getFormattedComponent(int value) {
        return getFormattedComponent(BigInteger.valueOf(value));
    }

    public MutableComponent getFormattedComponent(long value) {
        return getFormattedComponent(BigInteger.valueOf(value));
    }

    public MutableComponent getFormattedComponent(BigInteger value) {
        //  && !Screen.hasShiftDown()
        return (equals(FINAL) ? Component.literal("INFINITY") : EMCFormat.getComponent(value)).setStyle(ColorStyle.GREEN);
    }

    public MutableComponent getEMCLinkItemLimitComponent() {
        return getFormattedComponent(getEMCLinkItemLimit());
    }

    public MutableComponent getEMCLinkFluidLimitComponent() {
        return getFormattedComponent(getEMCLinkFluidLimit());
    }

    public MutableComponent getEMCLinkEMCLimitComponent() {
        return getFormattedComponent(getEMCLinkEMCLimit());
    }

    public MutableComponent getRelayTransferComponent() {
        return getFormattedComponent(getRelayTransferForTicks(Config.tickDelay.get()));
    }

    /* Registry Objects */

    public @Nullable Item getMatter() {
        return itemMatter == null ? null : itemMatter.get();
    }

    public @Nullable BlockPowerFlower getPowerFlower() {
        return powerFlower == null ? null : powerFlower.get();
    }

    public @Nullable BlockItem getPowerFlowerItem() {
        return itemPowerFlower == null ? null : itemPowerFlower.get();
    }

    public @Nullable BlockRelay getRelay() {
        return relay == null ? null : relay.get();
    }

    public @Nullable BlockItem getRelayItem() {
        return itemRelay == null ? null : itemRelay.get();
    }

    public @Nullable BlockCollector getCollector() {
        return collector == null ? null : collector.get();
    }

    public @Nullable BlockItem getCollectorItem() {
        return itemCollector == null ? null : itemCollector.get();
    }

    public @Nullable ItemCompressedEnergyCollector getCompressedCollectorItem() {
        return itemCompressedCollector == null ? null : itemCompressedCollector.get();
    }

    public @Nullable BlockEMCLink getEMCLink() {
        return emcLink == null ? null : emcLink.get();
    }

    public @Nullable BlockItem getEMCLinkItem() {
        return itemEMCLink == null ? null : itemEMCLink.get();
    }

    /* Registration */

    private void register(RegistrationType reg) {
        switch (reg) {
            case MATTER -> {
                if (hasItem) {
                    itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().tab(Main.tab).rarity(getRarity())));
                }
            }

            case MATTER_BLOCK -> {
                if (hasBlock) {
                    blockMatterBlock = Blocks.Registry.register(String.format("%s_matter_block", name), () -> new BlockMatter(this));
                    itemMatterBlock = Items.Registry.register(String.format("%s_matter_block", name), () -> new BlockItem(Objects.requireNonNull(blockMatterBlock).get(), new Item.Properties().tab(Main.tab).rarity(getRarity())));
                }
            }

            case COLLECTOR -> {
                collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(this));
                itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(collector).get(), new Item.Properties().tab(Main.tab).rarity(getRarity())));
            }

            case COMPRESSED_COLLECTOR -> itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedEnergyCollector(this));
            case POWER_FLOWER -> {
                powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(this));
                itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(powerFlower).get(), new Item.Properties().tab(Main.tab).rarity(getRarity())));
            }
            case RELAY -> {
                relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(this));
                itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(relay).get(), new Item.Properties().tab(Main.tab).rarity(getRarity())));
            }
            case EMC_LINK -> {
                emcLink = Blocks.Registry.register(String.format("%s_emc_link", name), () -> new BlockEMCLink(this));
                itemEMCLink = Items.Registry.register(String.format("%s_emc_link", name), () -> new BlockItem(Objects.requireNonNull(emcLink).get(), new Item.Properties().tab(Main.tab).rarity(getRarity())));
            }
        }
    }

    public static void registerAll() {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    private enum RegistrationType {
        MATTER,
        MATTER_BLOCK,
        COLLECTOR,
        COMPRESSED_COLLECTOR,
        POWER_FLOWER,
        RELAY,
        EMC_LINK

    }
}
