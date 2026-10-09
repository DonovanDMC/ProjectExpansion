package cool.furry.mc.forge.projectexpansion.gui.container.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PXCraftingContainer extends CraftingContainer {
    public PXCraftingContainer(AbstractContainerMenu menu, int width, int height) {
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
