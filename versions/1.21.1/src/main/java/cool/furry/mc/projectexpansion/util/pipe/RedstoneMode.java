package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum RedstoneMode implements StringRepresentable {
    IGNORED,
    OFF_WHEN_POWERED,
    ON_WHEN_POWERED,
    ALWAYS_OFF;

    public static final RedstoneMode[] VALUES = values();

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case OFF_WHEN_POWERED -> !powered;
            case ON_WHEN_POWERED -> powered;
            case ALWAYS_OFF -> false;
        };
    }

    public RedstoneMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
