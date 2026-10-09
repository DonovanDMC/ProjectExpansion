package cool.furry.mc.forge.projectexpansion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import moze_intel.projecte.integration.jei.collectors.CollectorRecipeCategory;
import moze_intel.projecte.integration.jei.collectors.FuelUpgradeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This mixin formats the EMC value shown for collectors in JEI
@SuppressWarnings("unused")
@Mixin(CollectorRecipeCategory.class)
public class CollectorRecipeCategoryMixin {
    @Final
    @Shadow(remap = false)
    private IDrawable arrow;

    @Shadow(remap = false)
    public IDrawable getBackground() {
        throw new IllegalStateException("Mixin failed to shadow getBackground()");
    }

    // I hate replacing the entire method, but I spent far too long trying to figure out how @ModifyVariable works
    @Inject(method = "draw(Lmoze_intel/projecte/integration/jei/collectors/FuelUpgradeRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lcom/mojang/blaze3d/vertex/PoseStack;DD)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void draw(FuelUpgradeRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack matrix, double mouseX, double mouseY, CallbackInfo ci) {
        Component emc = EMCFormat.getComponent(recipe.upgradeEMC());
        Font fontRenderer = Minecraft.getInstance().font;
        int stringWidth = fontRenderer.width(emc);
        fontRenderer.draw(matrix, emc, (float)(this.getBackground().getWidth() - stringWidth) / 2.0F, 5.0F, 8421504);
        this.arrow.draw(matrix, 55, 18);
        ci.cancel();
    }
}
