package cool.furry.mc.projectexpansion.util;

import com.google.common.collect.ImmutableList;
import cool.furry.mc.projectexpansion.block.*;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.item.ItemCompressedCollector;
import cool.furry.mc.projectexpansion.registries.Blocks;
import cool.furry.mc.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Matter implements IStringSerializable {
    BASIC(  0,  () -> MaterialColor.COLOR_GRAY, null, null, null, DyeColor.GRAY),
    DARK(   2,  () -> PEBlocks.DARK_MATTER.getBlock().defaultMaterialColor(), PEItems.DARK_MATTER, PEBlocks.DARK_MATTER::getBlock, EnumMatterType.DARK_MATTER, DyeColor.BLACK),
    RED(    4,  () -> PEBlocks.RED_MATTER.getBlock().defaultMaterialColor(), PEItems.RED_MATTER, PEBlocks.RED_MATTER::getBlock, EnumMatterType.RED_MATTER, DyeColor.RED),
    MAGENTA(4,  () -> MaterialColor.COLOR_MAGENTA, null,  null, null, DyeColor.MAGENTA),
    PINK(   5,  () -> MaterialColor.COLOR_PINK,  null,  null, null, DyeColor.PINK),
    PURPLE( 5,  () -> MaterialColor.COLOR_PURPLE,  null,  null, null, DyeColor.PURPLE),
    VIOLET( 6,  () -> MaterialColor.COLOR_PURPLE,  null,  null, null, DyeColor.PURPLE),
    BLUE(   6,  () -> MaterialColor.COLOR_BLACK,  null,  null, null, DyeColor.BLUE),
    CYAN(   7,  () -> MaterialColor.COLOR_CYAN,  null,  null, null, DyeColor.CYAN),
    GREEN(  7,  () -> MaterialColor.COLOR_GREEN,  null,  null, null, DyeColor.GREEN),
    LIME(   8,  () -> MaterialColor.COLOR_LIGHT_GREEN,  null,  null, null, DyeColor.LIME),
    YELLOW( 8,  () -> MaterialColor.COLOR_YELLOW,  null,  null, null, DyeColor.YELLOW),
    ORANGE( 9,  () -> MaterialColor.COLOR_ORANGE,  null,  null, null, DyeColor.ORANGE),
    WHITE(  9,  null,  null,  null, null, DyeColor.WHITE),
    FADING( 10,  () -> MaterialColor.COLOR_BLACK, null, null, null, DyeColor.GRAY),
    FINAL(  10,  null, Items.FINAL_STAR_SHARD::get, null, null, DyeColor.GRAY);
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

    public static final List<Matter> COMMON_ITEMS = ImmutableList.of(BASIC, DARK, RED);
    public static final List<Matter> UNCOMMON_ITEMS = ImmutableList.of(MAGENTA, PURPLE, VIOLET, BLUE);
    public static final List<Matter> RARE_ITEMS = ImmutableList.of(CYAN, GREEN, LIME, YELLOW);
    public static final List<Matter> EPIC_ITEMS = ImmutableList.of(ORANGE, WHITE, FADING, FINAL);


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
    public final Supplier<MaterialColor> mapColor;
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
    private RegistryObject<ItemCompressedCollector> itemCompressedCollector = null;
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
    @Nullable
    private EnumMatterType peMatterType = null;
    private final DyeColor color;
    Matter(int fluidEfficiency, @Nullable Supplier<MaterialColor> mapColor, @Nullable Supplier<Item> existingItem, @Nullable Supplier<Block> existingBlock, @Nullable EnumMatterType peMatterType, DyeColor color) {
        boolean isFinal = name().equals("FINAL"); // we can't access the FINAL member because we're in the constructor
        this.name = name().toLowerCase(Locale.US);
        this.hasItem = existingItem == null && ordinal() != 0;
        this.hasBlock = existingBlock == null && ordinal() != 0 && ordinal() != 15;
        this.level = ordinal() + 1;
        this.collectorOutputBase = getValue(BASE_COLLECTOR_OUTPUT);
        this.relayBonusBase = getValue(BASE_RELAY_BONUS);
        this.relayTransferBase = isFinal ? BigDecimal.valueOf(Long.MAX_VALUE) : getValue(BASE_RELAY_TRANSFER);
        this.fluidEfficiency = fluidEfficiency;
        this.mapColor = mapColor;
        this.existingItem = existingItem;
        this.existingBlock = existingBlock;
        this.peMatterType = peMatterType;
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    public int getTextColor() {
        return color.getTextColor();
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
        if(!Config.server.enableFluidEfficiency.get()) return 100;
        AtomicInteger efficiency = new AtomicInteger(fluidEfficiency);
        Arrays.stream(VALUES).filter((m) -> m.level < level).forEach((m) -> efficiency.addAndGet(m.fluidEfficiency));
        return efficiency.get();
    }

    /* Limits */
    public BigInteger getPowerFlowerOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(18)).add(relayBonusBase.multiply(BigDecimal.valueOf(30))).multiply(BigDecimal.valueOf(Config.server.powerflowerMultiplier.get())).toBigInteger();
    }

    public BigInteger getPowerFlowerOutputForTicks(int ticks) {
        if (ticks == 20) return getPowerFlowerOutput();
        BigInteger div20 = getPowerFlowerOutput().divide(BigInteger.valueOf(20));
        return div20.multiply(BigInteger.valueOf(ticks));
    }

    public BigInteger getCollectorOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(Config.server.collectorMultiplier.get())).toBigInteger();
    }

    public BigDecimal getCollectorOutputForTicks(int ticks) {
        if (ticks == 20) return new BigDecimal(getCollectorOutput());
        BigDecimal div20 = new BigDecimal(getCollectorOutput()).divide(BigDecimal.valueOf(20), 3, RoundingMode.UP);
        return div20.multiply(BigDecimal.valueOf(ticks));
    }

    /*
    unless we figure out a way to skip ticks or hard code numbers, dynamically changing the
    tick rate of these 2 will grossly duplicate emc
    */

    public BigInteger getRelayBonus() {
        return relayBonusBase.multiply(BigDecimal.valueOf(Config.server.relayBonusMultiplier.get())).toBigInteger();
    }

    public BigInteger getRelayBonusForTicks(int ticks) {
        return getRelayBonus();
    }

    public BigInteger getRelayTransfer() {
        return relayTransferBase.multiply(BigDecimal.valueOf(Config.server.relayTransferMultiplier.get())).toBigInteger();
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
                .multiply(BigDecimal.valueOf(Config.server.emcLinkEMCLimitMultiplier.get())).toBigInteger();
    }

    public int getEMCLinkItemLimit() {
        try {
            return BigDecimal.valueOf(2).pow(level - 1).multiply(BigDecimal.valueOf(Config.server.emcLinkItemLimitMultiplier.get())).intValueExact();
        } catch(ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public int getEMCLinkFluidLimit() {
        try {
            return BigDecimal.valueOf(2).pow(level - 1).multiply(BigDecimal.valueOf(1000)).multiply(BigDecimal.valueOf(Config.server.emcLinkFluidLimitMultiplier.get())).intValueExact();
        } catch(ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public IFormattableTextComponent getFormattedComponent(int value) {
        return getFormattedComponent(BigInteger.valueOf(value));
    }

    public IFormattableTextComponent getFormattedComponent(long value) {
        return getFormattedComponent(BigInteger.valueOf(value));
    }

    public IFormattableTextComponent getFormattedComponent(BigInteger value) {
        //  && !Screen.hasShiftDown()
        return (equals(FINAL) ? new StringTextComponent("INFINITY") : EMCFormat.getComponent(value)).setStyle(ColorStyle.GREEN);
    }

    public IFormattableTextComponent getEMCLinkItemLimitComponent() {
        return getFormattedComponent(getEMCLinkItemLimit());
    }

    public IFormattableTextComponent getEMCLinkFluidLimitComponent() {
        return getFormattedComponent(getEMCLinkFluidLimit());
    }

    public IFormattableTextComponent getEMCLinkEMCLimitComponent() {
        return getFormattedComponent(getEMCLinkEMCLimit());
    }

    public IFormattableTextComponent getRelayTransferComponent() {
        return getFormattedComponent(getRelayTransferForTicks(Config.server.tickDelay.get()));
    }

    /* Registry Objects */

    public @Nullable Item getMatter() {
        return itemMatter == null ? null : itemMatter.get();
    }

    public @Nullable Item getMatterOrExisting() {
        return itemMatter == null ? existingItem == null ? null : existingItem.get() : itemMatter.get();
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

    public @Nullable ItemCompressedCollector getCompressedCollectorItem() {
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
            case MATTER: {
                    if (hasItem) {
                        itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().rarity(getRarity())));
                    }
                } break;
            case MATTER_BLOCK: {
                    if (hasBlock) {
                        blockMatterBlock = Blocks.Registry.register(String.format("%s_matter_block", name), () -> new BlockMatter(BlockMatter.getProperties(this), this));
                        itemMatterBlock = Items.Registry.register(String.format("%s_matter_block", name), () -> new BlockItem(Objects.requireNonNull(blockMatterBlock).get(), new Item.Properties().rarity(getRarity())));
                    }
                } break;
            case COLLECTOR: {
                    collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(BlockCollector.getProperties(this), this));
                    itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(collector).get(), new Item.Properties().rarity(getRarity())));
                } break;
            case COMPRESSED_COLLECTOR: itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedCollector(this)); break;
            case POWER_FLOWER: {
                    powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(BlockPowerFlower.getProperties(this), this));
                    itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(powerFlower).get(), new Item.Properties().rarity(getRarity())));
                } break;
            case RELAY: {
                    relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(BlockRelay.getProperties(this), this));
                    itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(relay).get(), new Item.Properties().rarity(getRarity())));
                } break;
            case EMC_LINK: {
                    emcLink = Blocks.Registry.register(String.format("%s_emc_link", name), () -> new BlockEMCLink(BlockEMCLink.getProperties(this), this));
                    itemEMCLink = Items.Registry.register(String.format("%s_emc_link", name), () -> new BlockItem(Objects.requireNonNull(emcLink).get(), new Item.Properties().rarity(getRarity())));
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
        if (type == RegistrationType.MATTER && itemMatter != null) output.accept(itemMatter.get());
        if (type == RegistrationType.MATTER_BLOCK && itemMatterBlock != null) output.accept(itemMatterBlock.get());
        if (type == RegistrationType.COLLECTOR && itemCollector != null) output.accept(itemCollector.get());
        if (type == RegistrationType.COMPRESSED_COLLECTOR && itemCompressedCollector != null) output.accept(itemCompressedCollector.get());
        if (type == RegistrationType.POWER_FLOWER && itemPowerFlower != null) output.accept(itemPowerFlower.get());
        if (type == RegistrationType.RELAY && itemRelay != null) output.accept(itemRelay.get());
        if (type == RegistrationType.EMC_LINK && itemEMCLink != null) output.accept(itemEMCLink.get());
    }

    @Override
    public String getSerializedName() {
        return name.toLowerCase(Locale.US);
    }

    public int getMatterTier() {
        return ordinal() - 1;
    }

    public @Nullable EnumMatterType getPeMatterType() {
        return this.peMatterType;
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
