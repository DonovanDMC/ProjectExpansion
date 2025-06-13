package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.block.*;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.item.ItemCompressedCollector;
import cool.furry.mc.neoforge.projectexpansion.registries.Blocks;
import cool.furry.mc.neoforge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Matter implements StringRepresentable, IMatterType {
    BASIC(  0,  0, 0, 0, Util.EMPTY_TAG, () -> MapColor.COLOR_GRAY, null, null, DyeColor.GRAY),
    DARK(   2,  EnumMatterType.DARK_MATTER.getAttackDamageBonus(), EnumMatterType.DARK_MATTER.getSpeed(), EnumMatterType.DARK_MATTER.getChargeModifier(), EnumMatterType.DARK_MATTER.getIncorrectBlocksForDrops(), () -> PEBlocks.DARK_MATTER.getBlock().defaultMapColor(), PEItems.DARK_MATTER, PEBlocks.DARK_MATTER::getBlock, DyeColor.BLACK),
    RED(    4,  EnumMatterType.RED_MATTER.getAttackDamageBonus(), EnumMatterType.RED_MATTER.getSpeed(), EnumMatterType.RED_MATTER.getChargeModifier(), EnumMatterType.RED_MATTER.getIncorrectBlocksForDrops(), () -> PEBlocks.RED_MATTER.getBlock().defaultMapColor(), PEItems.RED_MATTER, PEBlocks.RED_MATTER::getBlock, DyeColor.RED),
    MAGENTA(4,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_MAGENTA, null,  null, DyeColor.MAGENTA),
    PINK(   5,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_PINK,  null,  null, DyeColor.PINK),
    PURPLE( 5,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_PURPLE,  null,  null, DyeColor.PURPLE),
    VIOLET( 6,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_PURPLE,  null,  null, DyeColor.PURPLE),
    BLUE(   6,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_BLACK,  null,  null, DyeColor.BLUE),
    CYAN(   7,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_CYAN,  null,  null, DyeColor.CYAN),
    GREEN(  7,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_GREEN,  null,  null, DyeColor.GREEN),
    LIME(   8,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_LIGHT_GREEN,  null,  null, DyeColor.LIME),
    YELLOW( 8,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_YELLOW,  null,  null, DyeColor.YELLOW),
    ORANGE( 9,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_ORANGE,  null,  null, DyeColor.ORANGE),
    WHITE(  9,  0, 0, 0, Util.EMPTY_TAG,  null,  null,  null, DyeColor.WHITE),
    FADING( 10,  0, 0, 0, Util.EMPTY_TAG,  () -> MapColor.COLOR_BLACK, null, null, DyeColor.GRAY),
    FINAL(  10,  0, 0, 0, Util.EMPTY_TAG,  null, Items.FINAL_STAR_SHARD::get, null, DyeColor.GRAY);
    public final BigDecimal BASE_COLLECTOR_OUTPUT = BigDecimal.valueOf(4L);
    public final BigDecimal BASE_RELAY_BONUS = BigDecimal.valueOf(1L);
    public final BigDecimal BASE_RELAY_TRANSFER = BigDecimal.valueOf(64L);

    public static final Matter[] VALUES = values();
    public static final StringRepresentable.StringRepresentableCodec<Matter> CODEC = StringRepresentable.fromEnum(Matter::values);

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
    public final float attackDamage;
    public final float efficiency;
    public final float chargeModifier;
    public final TagKey<Block> incorrectBlockForDrops;
    @Nullable
    public final Supplier<MapColor> mapColor;
    @Nullable
    public final Supplier<Item> existingItem;
    @Nullable
    public final Supplier<Block> existingBlock;
    @Nullable
    private DeferredHolder<Item, Item> itemMatter = null;
    @Nullable
    private DeferredHolder<Block, BlockPowerFlower> powerFlower = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> itemPowerFlower = null;
    @Nullable
    private DeferredHolder<Block, BlockCollector> collector = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> itemCollector = null;
    @Nullable
    private DeferredHolder<Item, ItemCompressedCollector> itemCompressedCollector = null;
    @Nullable
    private DeferredHolder<Block, BlockRelay> relay = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> itemRelay = null;
    @Nullable
    private DeferredHolder<Block, BlockEMCLink> emcLink = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> itemEMCLink = null;
    @Nullable
    private DeferredHolder<Item, BlockItem> itemMatterBlock = null;
    @Nullable
    private DeferredHolder<Block, BlockMatter> blockMatterBlock = null;
    private final DyeColor color;
    Matter(int fluidEfficiency, float attackDamage, float efficiency, float chargeModifier, TagKey<Block> incorrectBlockForDrops, @Nullable Supplier<MapColor> mapColor, @Nullable Supplier<Item> existingItem, @Nullable Supplier<Block> existingBlock, DyeColor color) {
        boolean isFinal = name().equals("FINAL"); // we can't access the FINAL member because we're in the constructor
        this.name = name().toLowerCase(Locale.US);
        this.hasItem = existingItem == null && ordinal() != 0;
        this.hasBlock = existingBlock == null && ordinal() != 0 && ordinal() != 15;
        this.level = ordinal() + 1;
        this.collectorOutputBase = getValue(BASE_COLLECTOR_OUTPUT);
        this.relayBonusBase = getValue(BASE_RELAY_BONUS);
        this.relayTransferBase = isFinal ? BigDecimal.valueOf(Long.MAX_VALUE) : getValue(BASE_RELAY_TRANSFER);
        this.fluidEfficiency = fluidEfficiency;
        this.attackDamage = attackDamage;
        this.efficiency = efficiency;
        this.chargeModifier = chargeModifier;
        this.incorrectBlockForDrops = incorrectBlockForDrops;
        this.mapColor = mapColor;
        this.existingItem = existingItem;
        this.existingBlock = existingBlock;
        this.color = color;
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

    public DyeColor getColor() {
        return color;
    }

    public int getTextColor() {
        return color.getTextColor();
    }

    public int getFluidEfficiencyPercentage() {
        if(!Config.server.enableFluidEfficiency.get()) return 100;
        AtomicInteger efficiency = new AtomicInteger(fluidEfficiency);
        Arrays.stream(VALUES).filter((m) -> m.level < level).forEach((m) -> efficiency.addAndGet(m.fluidEfficiency));
        return efficiency.get();
    }

    /* Limits */
    public BigInteger getPowerFlowerOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(18)).add(relayBonusBase.multiply(BigDecimal.valueOf(30))).multiply(BigDecimal.valueOf(Config.server.powerflowerMultiplier.get())).multiply(BigDecimal.valueOf(20)).toBigInteger();
    }

    public BigInteger getPowerFlowerOutputForTicks(int ticks) {
        if (ticks == 20) return getPowerFlowerOutput();
        BigInteger div20 = getPowerFlowerOutput().divide(BigInteger.valueOf(20));
        return div20.multiply(BigInteger.valueOf(ticks));
    }

    public BigInteger getCollectorOutput() {
        return collectorOutputBase.multiply(BigDecimal.valueOf(Config.server.collectorMultiplier.get())).multiply(BigDecimal.valueOf(20)).toBigInteger();
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
            case MATTER -> {
                if (hasItem) {
                    itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().rarity(getRarity())));
                }
            }

            case MATTER_BLOCK -> {
                if (hasBlock) {
                    blockMatterBlock = Blocks.Registry.register(String.format("%s_matter_block", name), () -> new BlockMatter(BlockMatter.getProperties(this), this));
                    itemMatterBlock = Items.Registry.register(String.format("%s_matter_block", name), () -> new BlockItem(Objects.requireNonNull(blockMatterBlock).get(), new Item.Properties().rarity(getRarity())));
                }
            }

            case COLLECTOR -> {
                collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(BlockCollector.getProperties(this), this));
                itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(collector).get(), new Item.Properties().rarity(getRarity())));
            }

            case COMPRESSED_COLLECTOR -> itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedCollector(this));
            case POWER_FLOWER -> {
                powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(BlockPowerFlower.getProperties(this), this));
                itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(powerFlower).get(), new Item.Properties().rarity(getRarity())));
            }
            case RELAY -> {
                relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(BlockRelay.getProperties(this), this));
                itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(relay).get(), new Item.Properties().rarity(getRarity())));
            }
            case EMC_LINK -> {
                emcLink = Blocks.Registry.register(String.format("%s_emc_link", name), () -> new BlockEMCLink(BlockEMCLink.getProperties(this), this));
                itemEMCLink = Items.Registry.register(String.format("%s_emc_link", name), () -> new BlockItem(Objects.requireNonNull(emcLink).get(), new Item.Properties().rarity(getRarity())));
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

    @Override
    public int getMatterTier() {
        return ordinal() - 1;
    }

    @Override
    public float getChargeModifier() {
        return chargeModifier;
    }

    @Override
    public int getUses() {
        return 0;
    }

    @Override
    public float getSpeed() {
        return efficiency;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlockForDrops;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
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
