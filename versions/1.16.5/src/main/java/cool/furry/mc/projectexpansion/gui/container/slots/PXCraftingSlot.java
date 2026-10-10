package cool.furry.mc.projectexpansion.gui.container.slots;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

// ghost items in this slot have to be paid for before they can leave it
public class PXCraftingSlot extends Slot {
    private final ContainerArcaneTransmutationTablet tablet;

    public PXCraftingSlot(ContainerArcaneTransmutationTablet tablet, IInventory container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.tablet = tablet;
    }

    @Override
    public boolean mayPickup(PlayerEntity player) {
        return tablet.canAffordGhosts(getSlotIndex());
    }

    // swapping and dragging also check this, and they can take the whole stack out
    @Override
    public boolean mayPlace(ItemStack stack) {
        return tablet.canAffordGhosts(getSlotIndex());
    }

    @Override
    public ItemStack remove(int amount) {
        tablet.payForTaken(getSlotIndex(), amount);
        return super.remove(amount);
    }

    // setByPlayer doesn't exist yet, set is only called for player actions on the server
    @Override
    public void set(ItemStack newStack) {
        // the stack being replaced takes its ghost items with it, growing the same stack in place doesn't
        if (newStack != getItem()) tablet.payForTaken(getSlotIndex(), getItem().getCount());
        super.set(newStack);
    }
}
