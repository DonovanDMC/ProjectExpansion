package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.util.SearchSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

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
