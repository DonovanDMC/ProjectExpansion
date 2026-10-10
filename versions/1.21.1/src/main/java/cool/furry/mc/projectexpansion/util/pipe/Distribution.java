package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

// the order emc is handed to the storages a pipe network reaches
public enum Distribution implements StringRepresentable {
    NEAREST,
    FURTHEST,
    ROUND_ROBIN,
    RANDOM;

    public static final Distribution[] VALUES = values();

    public Distribution next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
