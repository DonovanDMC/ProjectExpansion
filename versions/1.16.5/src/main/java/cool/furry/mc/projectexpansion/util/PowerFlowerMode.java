package cool.furry.mc.projectexpansion.util;

import java.util.Locale;

// what a power flower does with the emc it makes
public enum PowerFlowerMode {
    // goes to the owner, kept while they're offline
    PLAYER,
    // kept until something takes it
    STORAGE,
    // pushed into the blocks around it
    EJECT;

    public static final PowerFlowerMode[] VALUES = values();

    public static PowerFlowerMode byOrdinal(int ordinal) {
        return VALUES[Math.floorMod(ordinal, VALUES.length)];
    }

    // anything unknown, like a flower saved before modes existed, is the default
    public static PowerFlowerMode byName(String name) {
        for (PowerFlowerMode mode : VALUES) {
            if (mode.getSerializedName().equals(name)) return mode;
        }
        return PLAYER;
    }

    public PowerFlowerMode next() {
        return byOrdinal(ordinal() + 1);
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
