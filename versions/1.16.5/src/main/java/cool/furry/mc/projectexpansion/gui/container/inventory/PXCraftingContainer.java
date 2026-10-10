package cool.furry.mc.projectexpansion.gui.container.inventory;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PXCraftingContainer extends CraftingInventory {
    public PXCraftingContainer(Container menu, int width, int height) {
        super(menu, width, height);
    }

    public int size() {
        return getContainerSize();
    }

    // the items list isn't exposed before 1.20
    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>(getContainerSize());
        for (int i = 0; i < getContainerSize(); i++) {
            items.add(getItem(i));
        }
        return items;
    }
}
