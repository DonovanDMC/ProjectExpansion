package cool.furry.mc.projectexpansion.util.pipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// lives on the upgrade in a side, like Pipez, so moving the upgrade moves the settings
public record PipeSettings(RedstoneMode redstoneMode, Distribution distribution) {
    public static final PipeSettings DEFAULT = new PipeSettings(RedstoneMode.IGNORED, Distribution.ROUND_ROBIN);
    private static final String TAG = "PipeSettings";

    public static PipeSettings get(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag == null ? DEFAULT : load(tag);
    }

    public static boolean has(ItemStack stack) {
        return stack.getTagElement(TAG) != null;
    }

    public static void set(ItemStack stack, PipeSettings settings) {
        stack.addTagElement(TAG, settings.save());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("RedstoneMode", redstoneMode.getSerializedName());
        tag.putString("Distribution", distribution.getSerializedName());
        return tag;
    }

    public static PipeSettings load(CompoundTag tag) {
        return new PipeSettings(
                parse(RedstoneMode.VALUES, tag.getString("RedstoneMode"), DEFAULT.redstoneMode),
                parse(Distribution.VALUES, tag.getString("Distribution"), DEFAULT.distribution)
        );
    }

    private static <T extends Enum<T>> T parse(T[] values, String name, T fallback) {
        for (T value : values) {
            if (value.name().toLowerCase(Locale.ROOT).equals(name)) return value;
        }
        return fallback;
    }

    public PipeSettings withRedstoneMode(RedstoneMode mode) {
        return new PipeSettings(mode, distribution);
    }

    public PipeSettings withDistribution(Distribution value) {
        return new PipeSettings(redstoneMode, value);
    }

    // what actually applies, the upgrade has to unlock a setting for it to count
    public PipeSettings effective(@Nullable PipeUpgrade upgrade) {
        return new PipeSettings(
                PipeUpgrade.canChangeRedstoneMode(upgrade) ? redstoneMode : DEFAULT.redstoneMode,
                PipeUpgrade.canChangeDistribution(upgrade) ? distribution : DEFAULT.distribution
        );
    }
}
