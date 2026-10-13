package cool.furry.mc.projectexpansion.util.storage;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

// what a side of an emc storage does, changed with a wrench
public enum StorageSide implements StringRepresentable {
    INPUT,
    OUTPUT,
    OFF;

    private static final StorageSide[] VALUES = values();

    public StorageSide next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public StorageSide previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
