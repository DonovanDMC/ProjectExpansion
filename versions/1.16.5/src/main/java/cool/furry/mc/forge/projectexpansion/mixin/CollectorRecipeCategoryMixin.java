package cool.furry.mc.forge.projectexpansion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import mezz.jei.api.gui.drawable.IDrawable;
import moze_intel.projecte.integration.jei.collectors.CollectorRecipeCategory;
import moze_intel.projecte.integration.jei.collectors.FuelUpgradeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
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
    @Inject(method = "draw(Lmoze_intel/projecte/integration/jei/collectors/FuelUpgradeRecipe;Lcom/mojang/blaze3d/matrix/MatrixStack;DD)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void draw(FuelUpgradeRecipe recipe, MatrixStack matrix, double mouseX, double mouseY, CallbackInfo ci) {
        ITextComponent emc = EMCFormat.getComponent(recipe.getUpgradeEMC());
        FontRenderer fontRenderer = Minecraft.getInstance().font;
        int stringWidth = fontRenderer.width(emc);
        fontRenderer.draw(matrix, emc, (float)(this.getBackground().getWidth() - stringWidth) / 2.0F, 5.0F, 8421504);
        this.arrow.draw(matrix, 55, 18);
        ci.cancel();
    }
}
