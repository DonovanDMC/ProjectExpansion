package cool.furry.mc.projectexpansion.recipe;

import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

// a shaped recipe that keeps the emc stored in the emc storage it's made from
public class EmcStorageUpgradeRecipe extends ShapedRecipe {
    // made from the shaped recipe its serializer reads
    public EmcStorageUpgradeRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack output = super.assemble(container, registries);
        BlockEmcStorage.copyStoredEmc(container, output);
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypes.EMC_STORAGE_UPGRADE_SERIALIZER.get();
    }
}
