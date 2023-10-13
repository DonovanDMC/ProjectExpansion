package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.item.ItemCompressedEnergyCollector;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Matter {
    BASIC(  0,  null, Lang.Blocks.BASIC_COLLECTOR),
    DARK(   2,  PEItems.DARK_MATTER, Lang.Blocks.DARK_COLLECTOR),
    RED(    4,  PEItems.RED_MATTER, Lang.Blocks.RED_COLLECTOR),
    MAGENTA(4,  null, Lang.Blocks.MAGENTA_COLLECTOR),
    PINK(   5,  null, Lang.Blocks.PINK_COLLECTOR),
    PURPLE( 5,  null, Lang.Blocks.PURPLE_COLLECTOR),
    VIOLET( 6,  null, Lang.Blocks.VIOLET_COLLECTOR),
    BLUE(   6,  null, Lang.Blocks.BLUE_COLLECTOR),
    CYAN(   7,  null, Lang.Blocks.CYAN_COLLECTOR),
    GREEN(  7,  null, Lang.Blocks.GREEN_COLLECTOR),
    LIME(   8,  null, Lang.Blocks.LIME_COLLECTOR),
    YELLOW( 8,  null, Lang.Blocks.YELLOW_COLLECTOR),
    ORANGE( 9,  null, Lang.Blocks.ORANGE_COLLECTOR),
    WHITE(  9,  null, Lang.Blocks.WHITE_COLLECTOR),
    FADING( 10, null, Lang.Blocks.FADING_COLLECTOR),
    FINAL(  10, Items.FINAL_STAR_SHARD, Lang.Blocks.FINAL_COLLECTOR);
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

    public final String name;
    public final boolean hasItem;
    public final int level;
    public final BigDecimal collectorOutputBase;
    public final BigDecimal relayBonusBase;
    public final BigDecimal relayTransferBase;
    /** @deprecated Due to how 1.19.2 config values work, this will not be set to 100 when fluid efficiency is disabled. */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public final int fluidEfficiency;
    @Nullable
    public final Supplier<Item> existingItem;
    public final Rarity rarity;
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
    private ILangEntry collectorLang = null;

    public static final int UNCOMMON_THRESHOLD = 4;
    public static final int RARE_THRESHOLD = 15;
    public static final int EPIC_THRESHOLD = 16;
    Matter(int fluidEfficiency, @Nullable Supplier<Item> existingItem, ILangEntry collectorLang) {
        boolean isFinal = name().equals("FINAL"); // we can't access the FINAL member because we're in the constructor
        this.name = name().toLowerCase(Locale.US);
        this.hasItem = existingItem == null && ordinal() != 0;
        this.level = ordinal() + 1;
        this.collectorOutputBase = getValue(BASE_COLLECTOR_OUTPUT);
        this.relayBonusBase = getValue(BASE_RELAY_BONUS);
        this.relayTransferBase = isFinal ? BigDecimal.valueOf(Long.MAX_VALUE) : getValue(BASE_RELAY_TRANSFER);
        this.fluidEfficiency = fluidEfficiency;
        this.existingItem = existingItem;
        this.collectorLang = collectorLang;
        this.rarity =
                level >= EPIC_THRESHOLD ? Rarity.EPIC :
                        level >= RARE_THRESHOLD ? Rarity.RARE :
                                level >= UNCOMMON_THRESHOLD ? Rarity.UNCOMMON :
                                        Rarity.COMMON;
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

    public @Nullable ILangEntry getCollectorLang() {
        return collectorLang;
    }

    /* Registration */

    private void register(RegistrationType reg) {
        switch (reg) {
            case MATTER: {
                if (hasItem) {
                    itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().tab(Main.tab).rarity(rarity)));
                }
                break;
            }

            case COLLECTOR: {
                collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(this));
                itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(collector).get()), new Item.Properties().tab(Main.tab).rarity(rarity)));
                break;
            }

            case COMPRESSED_COLLECTOR: {
                itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedEnergyCollector(this));
                break;
            }

            case POWER_FLOWER: {
                powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(this));
                itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(powerFlower).get()), new Item.Properties().tab(Main.tab).rarity(rarity)));
                break;
            }

            case RELAY: {
                relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(this));
                itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(relay).get()), new Item.Properties().tab(Main.tab).rarity(rarity)));
                break;
            }

            case EMC_LINK: {
                emcLink = Blocks.Registry.register(String.format("%s_emc_link", name), () -> new BlockEMCLink(this));
                itemEMCLink = Items.Registry.register(String.format("%s_emc_link", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(emcLink).get()), new Item.Properties().tab(Main.tab).rarity(rarity)));
                break;
            }
        }
    }

    public static void registerAll() {
        Arrays.stream(RegistrationType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    private enum RegistrationType {
        MATTER,
        COLLECTOR,
        COMPRESSED_COLLECTOR,
        POWER_FLOWER,
        RELAY,
        EMC_LINK

    }
}
