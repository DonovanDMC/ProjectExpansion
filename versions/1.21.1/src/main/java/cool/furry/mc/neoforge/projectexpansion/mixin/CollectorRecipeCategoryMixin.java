package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import moze_intel.projecte.integration.recipe_viewer.FuelUpgradeRecipe;
import moze_intel.projecte.integration.recipe_viewer.jei.CollectorRecipeCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This mixin formats the EMC value shown for collectors in JEI
@SuppressWarnings("unused")
@Mixin(value = CollectorRecipeCategory.class, remap = false)
public class CollectorRecipeCategoryMixin {
    @Shadow(remap = false)
    public int getWidth() {
        throw new IllegalStateException("Mixin failed to shadow getWidth()");
    }

    // I hate replacing the entire method, but I spent far too long trying to figure out how @ModifyVariable works
    @Inject(method = "createRecipeExtras(Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;Lmoze_intel/projecte/integration/recipe_viewer/FuelUpgradeRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void draw(IRecipeExtrasBuilder builder, FuelUpgradeRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        builder.addRecipeArrow().setPosition(27, 16);
        builder.addText(EMCFormat.getComponent(recipe.upgradeEMC()), getWidth() - 10, 11)
                .setPosition(5, 5)
                .setTextAlignment(HorizontalAlignment.CENTER);
        ci.cancel();
    }
}
