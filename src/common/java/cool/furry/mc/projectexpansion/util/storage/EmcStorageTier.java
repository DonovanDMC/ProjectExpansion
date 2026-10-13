package cool.furry.mc.projectexpansion.util.storage;

import cool.furry.mc.projectexpansion.config.Config;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Locale;
import java.util.function.Supplier;

// named like the pipe upgrades, and each is crafted from the tier before with the same matter
public enum EmcStorageTier implements StringRepresentable {
    BASIC(() -> Config.server.emcStorageCapacityBasic.get(), () -> Config.server.emcStorageRateBasic.get()),
    IMPROVED(() -> Config.server.emcStorageCapacityImproved.get(), () -> Config.server.emcStorageRateImproved.get()),
    ADVANCED(() -> Config.server.emcStorageCapacityAdvanced.get(), () -> Config.server.emcStorageRateAdvanced.get()),
    ULTIMATE(() -> Config.server.emcStorageCapacityUltimate.get(), () -> Config.server.emcStorageRateUltimate.get()),
    INFINITY(null, null);

    public static final EmcStorageTier[] VALUES = values();
    // stands in for unlimited, far past anything that could be stored or moved
    public static final BigInteger UNLIMITED = BigInteger.TEN.pow(100);

    private final @Nullable Supplier<Long> capacity;
    private final @Nullable Supplier<Long> rate;

    EmcStorageTier(@Nullable Supplier<Long> capacity, @Nullable Supplier<Long> rate) {
        this.capacity = capacity;
        this.rate = rate;
    }

    public boolean isUnlimited() {
        return capacity == null;
    }

    public BigInteger getCapacity() {
        return capacity == null ? UNLIMITED : BigInteger.valueOf(capacity.get());
    }

    // emc each tick, shared between every output side
    public BigInteger getRate() {
        return rate == null ? UNLIMITED : BigInteger.valueOf(rate.get());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
