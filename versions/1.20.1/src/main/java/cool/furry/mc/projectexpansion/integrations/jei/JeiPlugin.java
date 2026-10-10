package cool.furry.mc.projectexpansion.integrations.jei;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.SearchSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    public static IJeiRuntime RUNTIME;

    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RefiningCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        registration.addRecipes(RefiningCategory.TYPE, Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(cool.furry.mc.projectexpansion.registries.RecipeTypes.REFINING.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.EMC_REFINERY.get()), RefiningCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArcaneCraftingTransferHandler(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GUIArcaneTransmutationTablet.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(GUIArcaneTransmutationTablet screen) {
                return screen.getExtraAreas();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RUNTIME = jeiRuntime;
        SearchSync.register(new SearchSync("jei", text -> {
            if (RUNTIME != null) RUNTIME.getIngredientFilter().setFilterText(text);
        }));
    }

    @Override
    public void onRuntimeUnavailable() {
        RUNTIME = null;
    }
}
