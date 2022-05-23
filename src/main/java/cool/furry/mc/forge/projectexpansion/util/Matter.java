package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.Blocks;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemCompressedEnergyCollector;
import moze_intel.projecte.gameObjs.ObjHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Matter {
    BASIC(  4L,             1L,             64L,            null),
    DARK(   12L,            3L,             192L,           ObjHandler.darkMatter::getItem),
    RED(    40L,            10L,            640L,           ObjHandler.redMatter::getItem),
    MAGENTA(160L,           40L,            2560L,          null),
    PINK(   640L,           150L,           10240L,         null),
    PURPLE( 2560L,          750L,           40960L,         null),
    VIOLET( 10240L,         3750L,          163840L,        null),
    BLUE(   40960L,         15000L,         655360L,        null),
    CYAN(   163840L,        60000L,         2621440L,       null),
    GREEN(  655360L,        240000L,        10485760L,      null),
    LIME(   2621440L,       960000L,        41943040L,      null),
    YELLOW( 10485760L,      3840000L,       167772160L,     null),
    ORANGE( 41943040L,      15360000L,      671088640L,     null),
    WHITE(  167772160L,     61440000L,      2684354560L,    null),
    FADING( 671088640L,     245760000L,     10737418240L,   null),
    FINAL(  1000000000000L, 1000000000000L, Long.MAX_VALUE, Items.FINAL_STAR_SHARD);

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
    public final BigInteger collectorOutputBase;
    public final BigInteger relayBonusBase;
    public final BigInteger relayTransferBase;
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

    public static final int UNCOMMON_THRESHOLD = 4;
    public static final int RARE_THRESHOLD = 15;
    public static final int EPIC_THRESHOLD = 16;
    Matter(long collectorOutput, long relayBonus, long relayTransfer, @Nullable Supplier<Item> existingItem) {
        this.name = name().toLowerCase();
        this.hasItem = existingItem == null && ordinal() != 0;
        this.level = ordinal() + 1;
        // Gₙ(aₙ)(z)=4(2z²+z-1)/4z-1
        this.collectorOutputBase = Config.useOldValues.get() ? calcOldValue(BigInteger.valueOf(6), level) : BigInteger.valueOf(collectorOutput);
        // ¯\_(ツ)_/¯
        this.relayBonusBase = Config.useOldValues.get() ? calcOldValue(BigInteger.valueOf(1), level) : BigInteger.valueOf(relayBonus);
        // Gₙ(aₙ)(z)=64(2z²+z-1)/4z-1
        this.relayTransferBase = Config.useOldValues.get() ? calcOldValue(BigInteger.valueOf(64), level) : BigInteger.valueOf(relayTransfer);
        this.existingItem = existingItem;
        this.rarity =
            level >= EPIC_THRESHOLD ? Rarity.EPIC :
                level >= RARE_THRESHOLD ? Rarity.RARE :
                    level >= UNCOMMON_THRESHOLD ? Rarity.UNCOMMON :
                        Rarity.COMMON;
    }

    private BigInteger calcOldValue(BigInteger base, int level) {
        BigInteger i = base;
        for (int v = 1; v <= level; v++) i = i.multiply(BigInteger.valueOf(v));
        return i;
    }

    public int getLevel() {
        return level;
    }

    /* Limits */

    public BigInteger getPowerFlowerOutput() {
        return collectorOutputBase.multiply(BigInteger.valueOf(18)).add(relayBonusBase.multiply(BigInteger.valueOf(30))).multiply(BigInteger.valueOf(Config.powerflowerMultiplier.get()));
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
        return collectorOutputBase.multiply(BigInteger.valueOf(Config.collectorMultiplier.get()));
    }

    public BigInteger getCollectorOutputForTicks(int ticks) {
        return getCollectorOutput();
    }

    public BigInteger getRelayBonus() {
        return relayBonusBase.multiply(BigInteger.valueOf(Config.relayBonusMultiplier.get()));
    }

    public BigInteger getRelayBonusForTicks(int ticks) {
        return getRelayBonus();
    }

    public BigInteger getRelayTransfer() {
        return relayTransferBase.multiply(BigInteger.valueOf(Config.relayTransferMultiplier.get()));
    }
    public BigInteger getRelayTransferForTicks(int ticks) {
        return getRelayTransfer();
    }

    public int getEMCLinkInventorySize() {
        return level * 3;
    }

    public BigInteger getEMCLinkEMCLimit() {
        return BigInteger.valueOf(16)
            .pow(level)
            .multiply(BigInteger.valueOf(Config.emcLinkEMCLimitMultiplier.get()));
    }

    public int getEMCLinkItemLimit() {
        try {
            return Math.multiplyExact((int) Math.pow(2, level - 1), Config.emcLinkItemLimitMultiplier.get());
        } catch(ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public StringTextComponent getEMCLinkItemLimitComponent() {
        return new StringTextComponent(this == FINAL ? "INFINITY" : String.valueOf(getEMCLinkItemLimit()));
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
            case MATTER: {
                if (hasItem)
                    itemMatter = Items.Registry.register(String.format("%s_matter", name), () -> new Item(new Item.Properties().group(Main.group).rarity(rarity)));
                break;
            }

            case COLLECTOR: {
                collector = Blocks.Registry.register(String.format("%s_collector", name), () -> new BlockCollector(this));
                itemCollector = Items.Registry.register(String.format("%s_collector", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(collector).get()), new Item.Properties().group(Main.group).rarity(rarity)));
                break;
            }

            case COMPRESSED_COLLECTOR: {
                itemCompressedCollector = Items.Registry.register(String.format("%s_compressed_collector", name), () -> new ItemCompressedEnergyCollector(this));
                break;
            }

            case POWER_FLOWER: {
                powerFlower = Blocks.Registry.register(String.format("%s_power_flower", name), () -> new BlockPowerFlower(this));
                itemPowerFlower = Items.Registry.register(String.format("%s_power_flower", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(powerFlower).get()), new Item.Properties().group(Main.group).rarity(rarity)));
                break;
            }

            case RELAY: {
                relay = Blocks.Registry.register(String.format("%s_relay", name), () -> new BlockRelay(this));
                itemRelay = Items.Registry.register(String.format("%s_relay", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(relay).get()), new Item.Properties().group(Main.group).rarity(rarity)));
                break;
            }

            case EMC_LINK: {
                emcLink = Blocks.Registry.register(String.format("%s_emc_link", name), () -> new BlockEMCLink(this));
                itemEMCLink = Items.Registry.register(String.format("%s_emc_link", name), () -> new BlockItem(Objects.requireNonNull(Objects.requireNonNull(emcLink).get()), new Item.Properties().group(Main.group).rarity(rarity)));
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
