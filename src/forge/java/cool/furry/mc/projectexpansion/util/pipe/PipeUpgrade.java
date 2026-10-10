package cool.furry.mc.projectexpansion.util.pipe;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Platform;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

// the tiers match Pipez's upgrades, and the tags include them so they work in our pipes too
public enum PipeUpgrade implements StringRepresentable {
    BASIC(true, false, () -> Config.server.emcPipeRateBasic.get()),
    IMPROVED(true, true, () -> Config.server.emcPipeRateImproved.get()),
    ADVANCED(true, true, () -> Config.server.emcPipeRateAdvanced.get()),
    ULTIMATE(true, true, () -> Config.server.emcPipeRateUltimate.get()),
    INFINITY(true, true, () -> Long.MAX_VALUE);

    public static final PipeUpgrade[] VALUES = values();

    public final boolean canChangeRedstoneMode;
    public final boolean canChangeDistribution;
    private final Supplier<Long> rate;
    public final TagKey<Item> tag;

    PipeUpgrade(boolean canChangeRedstoneMode, boolean canChangeDistribution, Supplier<Long> rate) {
        this.canChangeRedstoneMode = canChangeRedstoneMode;
        this.canChangeDistribution = canChangeDistribution;
        this.rate = rate;
        this.tag = TagKey.create(Platform.ITEM, Main.rl("pipe_upgrades/" + getSerializedName()));
    }

    // emc per tick
    public long getRate() {
        return rate.get();
    }

    public static long getRate(@Nullable PipeUpgrade upgrade) {
        return upgrade == null ? Config.server.emcPipeRate.get() : upgrade.getRate();
    }

    // the best tier the stack is tagged as, an item could be in more than one
    public static @Nullable PipeUpgrade fromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (int i = VALUES.length - 1; i >= 0; i--) {
            if (stack.is(VALUES[i].tag)) return VALUES[i];
        }
        return null;
    }

    public static boolean canChangeRedstoneMode(@Nullable PipeUpgrade upgrade) {
        return upgrade != null && upgrade.canChangeRedstoneMode;
    }

    public static boolean canChangeDistribution(@Nullable PipeUpgrade upgrade) {
        return upgrade != null && upgrade.canChangeDistribution;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
