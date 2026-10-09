package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

// stored directly in the stack's nbt, using the same keys as older versions
public final class OwnerData {
    private final UUID uuid;
    private final String name;

    public OwnerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OwnerData that = (OwnerData) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }

    @Override
    public String toString() {
        return "OwnerData[uuid=" + uuid + ", name=" + name + "]";
    }

    public boolean isNone() {
        return uuid.equals(Util.DUMMY_UUID);
    }

    public static boolean has(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.hasUUID(TagNames.OWNER);
    }

    public static @Nullable OwnerData get(ItemStack stack) {
        if (!has(stack)) return null;
        CompoundNBT tag = stack.getOrCreateTag();
        return new OwnerData(tag.getUUID(TagNames.OWNER), tag.getString(TagNames.OWNER_NAME));
    }

    public void set(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putUUID(TagNames.OWNER, uuid);
        tag.putString(TagNames.OWNER_NAME, name);
    }

    public static void remove(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null) return;
        tag.remove(TagNames.OWNER);
        tag.remove(TagNames.OWNER_NAME);
    }
}
