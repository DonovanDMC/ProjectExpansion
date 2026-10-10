package cool.furry.mc.projectexpansion.integrations.jei;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RefiningCategory implements IRecipeCategory<RecipeHolder<RefiningRecipe>> {
    public static final RecipeType<RecipeHolder<RefiningRecipe>> TYPE = RecipeType.createRecipeHolderType(Main.rl("refining"));
    private final IGuiHelper helper;
    private final IDrawable icon;
    // the arrow takes as long as the recipe does
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public RefiningCategory(IGuiHelper helper) {
        this.helper = helper;
        this.icon = helper.createDrawableItemLike(Items.EMC_REFINERY.get());
    }

    @Override
    public RecipeType<RecipeHolder<RefiningRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Lang.GUI.REFINING.translate();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 82;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RefiningRecipe> holder, IFocusGroup focuses) {
        RefiningRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).setStandardSlotBackground()
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems()).map(stack -> stack.copyWithCount(recipe.count())).toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 1).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<RefiningRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        RefiningRecipe recipe = holder.value();
        arrows.computeIfAbsent(recipe.time(), helper::createAnimatedRecipeArrow).draw(graphics, 24, 1);
        Component text = Lang.GUI.EMC_REFINERY_TIME.translate(EMCFormat.getComponent(recipe.emc()), recipe.time() / 20.0);
        graphics.drawString(Minecraft.getInstance().font, text, (getWidth() - Minecraft.getInstance().font.width(text)) / 2, 23, 0x808080, false);
    }
}
