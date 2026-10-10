package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

// what an emc pipe does on one of its sides, kept in the block state so it can be rendered without a block entity renderer
public enum PipeSide implements StringRepresentable {
    NONE,
    CONNECTED,
    EXTRACTING;

    public boolean isConnected() {
        return this != NONE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
