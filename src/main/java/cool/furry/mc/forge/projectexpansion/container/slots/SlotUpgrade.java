package cool.furry.mc.forge.projectexpansion.container.slots;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class SlotUpgrade extends Slot {
    private final ItemUpgrade.UpgradeType type;
    public SlotUpgrade(IInventory inv, int index, int x, int y, ItemUpgrade.UpgradeType type) {
        super(inv, index, x, y);
        this.type = type;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return type == ItemUpgrade.UpgradeType.SPEED && stack.getItem().equals(Items.SPEED_UPGRADE.get()) ||
            type == ItemUpgrade.UpgradeType.STACK && stack.getItem().equals(Items.STACK_UPGRADE.get());
    }
}
