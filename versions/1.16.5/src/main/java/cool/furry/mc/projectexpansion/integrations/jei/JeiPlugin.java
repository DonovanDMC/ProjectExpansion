package cool.furry.mc.projectexpansion.integrations.jei;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.util.SearchSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.util.ResourceLocation;

import java.util.List;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    public static IJeiRuntime RUNTIME;

    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArcaneCraftingTransferHandler(), VanillaRecipeCategoryUid.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GUIArcaneTransmutationTablet.class, new IGuiContainerHandler<GUIArcaneTransmutationTablet>() {
            @Override
            public List<Rectangle2d> getGuiExtraAreas(GUIArcaneTransmutationTablet screen) {
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
}
