package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum RedstoneMode implements IStringSerializable {
    IGNORED,
    OFF_WHEN_POWERED,
    ON_WHEN_POWERED,
    ALWAYS_OFF;

    public static final RedstoneMode[] VALUES = values();

    public boolean allows(boolean powered) {
        switch (this) {
            case IGNORED: return true;
            case OFF_WHEN_POWERED: return !powered;
            case ON_WHEN_POWERED: return powered;
            default: return false;
        }
    }

    public RedstoneMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
