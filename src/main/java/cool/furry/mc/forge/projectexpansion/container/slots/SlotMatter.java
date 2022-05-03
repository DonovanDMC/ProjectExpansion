package cool.furry.mc.forge.projectexpansion.container.slots;

import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import java.util.Arrays;

public class SlotMatter extends Slot {
    public SlotMatter(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return Util.isMatter(stack);
    }
}
