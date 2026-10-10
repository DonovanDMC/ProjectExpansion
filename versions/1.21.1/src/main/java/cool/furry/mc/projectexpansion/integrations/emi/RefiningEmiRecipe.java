package cool.furry.mc.projectexpansion.integrations.emi;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class RefiningEmiRecipe extends BasicEmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(Main.rl("refining"), EmiStack.of(Items.EMC_REFINERY.get()));
    private final RefiningRecipe recipe;

    public RefiningEmiRecipe(RecipeHolder<RefiningRecipe> holder) {
        super(CATEGORY, holder.id(), 82, 32);
        this.recipe = holder.value();
        inputs = List.of(EmiIngredient.of(recipe.ingredient(), recipe.count()));
        outputs = List.of(EmiStack.of(recipe.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 0, 0);
        // the arrow takes as long as the recipe does, in milliseconds
        widgets.addFillingArrow(24, 0, recipe.time() * 50);
        widgets.addSlot(outputs.getFirst(), 58, 0).recipeContext(this);
        widgets.addText(Lang.GUI.EMC_REFINERY_TIME.translate(EMCFormat.getComponent(recipe.emc()), recipe.time() / 20.0), 41, 22, 0x808080, false).horizontalAlign(TextWidget.Alignment.CENTER);
    }
}
