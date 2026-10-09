package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// stored directly in the stack's nbt, using the same keys as older versions
public record OwnerData(UUID uuid, String name) {
    public boolean isNone() {
        return uuid.equals(Util.DUMMY_UUID);
    }

    public static boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TagNames.OWNER);
    }

    public static @Nullable OwnerData get(ItemStack stack) {
        if (!has(stack)) return null;
        CompoundTag tag = stack.getOrCreateTag();
        return new OwnerData(tag.getUUID(TagNames.OWNER), tag.getString(TagNames.OWNER_NAME));
    }

    public void set(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TagNames.OWNER, uuid);
        tag.putString(TagNames.OWNER_NAME, name);
    }

    public static void remove(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(TagNames.OWNER);
        tag.remove(TagNames.OWNER_NAME);
    }
}
