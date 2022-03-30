package cool.furry.mc.forge.projectexpansion.container.slots;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class SlotEMC extends Slot {
    @Nullable
    private int maxItems = 0;

    public SlotEMC(IInventory inv, int index, int x, int y, int maxItems) {
        super(inv, index, x, y);
        this.maxItems = maxItems;
    }

    public SlotEMC(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return ProjectEAPI.getEMCProxy().hasValue(stack.getItem());
    }

    @Override
    public int getSlotStackLimit() {
        return maxItems == 0 ? super.getSlotStackLimit() : maxItems;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return maxItems == 0 ? super.getItemStackLimit(stack) : getSlotStackLimit();
    }
}
