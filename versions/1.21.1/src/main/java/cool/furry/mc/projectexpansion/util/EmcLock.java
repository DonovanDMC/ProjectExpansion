package cool.furry.mc.projectexpansion.util;

import cool.furry.mc.projectexpansion.registries.DataComponentTypes;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

public class EmcLock {
    public static boolean isLocked(ItemStack stack) {
        return stack.has(DataComponentTypes.EMC_LOCKED.get());
    }

    public static boolean canToggle(ItemStack stack) {
        return !stack.isEmpty() && (isLocked(stack) || IEMCProxy.INSTANCE.hasValue(stack));
    }

    public static void toggle(ItemStack stack) {
        if (isLocked(stack)) {
            stack.remove(DataComponentTypes.EMC_LOCKED.get());
        } else {
            stack.set(DataComponentTypes.EMC_LOCKED.get(), Unit.INSTANCE);
        }
    }
}
