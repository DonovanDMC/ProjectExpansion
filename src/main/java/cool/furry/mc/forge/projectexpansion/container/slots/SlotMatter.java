package cool.furry.mc.forge.projectexpansion.container.slots;

import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

public class SlotMatter extends SlotItemHandler {
    public SlotMatter(IItemHandlerModifiable itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return Util.isMatter(stack);
    }
}
