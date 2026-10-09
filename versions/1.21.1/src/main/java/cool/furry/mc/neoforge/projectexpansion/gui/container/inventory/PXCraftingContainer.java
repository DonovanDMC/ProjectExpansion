package cool.furry.mc.neoforge.projectexpansion.gui.container.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeInput;

public class PXCraftingContainer extends TransientCraftingContainer implements RecipeInput {
    public PXCraftingContainer(AbstractContainerMenu menu, int width, int height) {
        super(menu, width, height);
    }

    @Override
    public int size() {
        return getContainerSize();
    }
}
