package cool.furry.mc.projectexpansion.gui.container.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;

public class PXCraftingContainer extends TransientCraftingContainer {
    public PXCraftingContainer(AbstractContainerMenu menu, int width, int height) {
        super(menu, width, height);
    }

    public int size() {
        return getContainerSize();
    }
}
