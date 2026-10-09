package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class EmcLock {
    // presence means the stack can't be put into anything that converts it to EMC
    public static boolean isLocked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TagNames.EMC_LOCKED);
    }

    public static boolean canToggle(ItemStack stack) {
        return !stack.isEmpty() && (isLocked(stack) || IEMCProxy.INSTANCE.hasValue(stack));
    }

    public static void toggle(ItemStack stack) {
        if (isLocked(stack)) {
            stack.removeTagKey(TagNames.EMC_LOCKED);
        } else {
            stack.getOrCreateTag().putBoolean(TagNames.EMC_LOCKED, true);
        }
    }
}
