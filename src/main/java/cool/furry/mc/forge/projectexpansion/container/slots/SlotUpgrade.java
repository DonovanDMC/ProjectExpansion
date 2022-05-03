package cool.furry.mc.forge.projectexpansion.container.slots;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public class SlotUpgrade extends SlotItemHandler {
    private final ItemUpgrade.UpgradeType type;
    public SlotUpgrade(IItemHandlerModifiable itemHandler, int index, int x, int y, ItemUpgrade.UpgradeType type) {
        super(itemHandler, index, x, y);
        this.type = type;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return type == ItemUpgrade.UpgradeType.SPEED && stack.getItem().equals(Items.SPEED_UPGRADE.get()) ||
            type == ItemUpgrade.UpgradeType.STACK && stack.getItem().equals(Items.STACK_UPGRADE.get());
    }
}
