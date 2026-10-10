package cool.furry.mc.projectexpansion.integrations.emi;

import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.registries.RecipeTypes;
import cool.furry.mc.projectexpansion.util.SearchSync;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        SearchSync.register(new SearchSync("emi", EmiApi::setSearchText));
        registry.addCategory(RefiningEmiRecipe.CATEGORY);
        registry.addWorkstation(RefiningEmiRecipe.CATEGORY, EmiStack.of(Items.EMC_REFINERY.get()));
        for (RecipeHolder<RefiningRecipe> recipe : registry.getRecipeManager().getAllRecipesFor(RecipeTypes.REFINING.get())) registry.addRecipe(new RefiningEmiRecipe(recipe));
        registry.addRecipeHandler(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), new ArcaneCraftingTransferHandler());
        registry.addExclusionArea(GUIArcaneTransmutationTablet.class, (screen, consumer) -> {
            for (Rect2i area : screen.getExtraAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
    }
}
