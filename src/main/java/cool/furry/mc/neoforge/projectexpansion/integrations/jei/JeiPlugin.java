package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.util.SearchSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    public static IJeiRuntime RUNTIME;

    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArcaneCraftingTransferHandler(), RecipeTypes.CRAFTING);
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
