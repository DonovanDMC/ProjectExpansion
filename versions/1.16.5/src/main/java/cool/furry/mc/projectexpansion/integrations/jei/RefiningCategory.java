package cool.furry.mc.projectexpansion.integrations.jei;

import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RefiningCategory implements IRecipeCategory<RefiningRecipe> {
    public static final ResourceLocation UID = Main.rl("refining");
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/emc_refinery.png");
    private static final int WIDTH = 82, HEIGHT = 32;
    private final IGuiHelper helper;
    private final IDrawable background;
    private final IDrawable icon;
    // the refinery's own arrow, empty and filled
    private final IDrawableStatic emptyArrow;
    private final IDrawableStatic filledArrow;
    // the arrow takes as long as the recipe does
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public RefiningCategory(IGuiHelper helper) {
        this.helper = helper;
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(new ItemStack(Items.EMC_REFINERY.get()));
        this.emptyArrow = helper.createDrawable(TEXTURE, 79, 34, 24, 17);
        this.filledArrow = helper.createDrawable(TEXTURE, 176, 0, 24, 17);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends RefiningRecipe> getRecipeClass() {
        return RefiningRecipe.class;
    }

    @Override
    public String getTitle() {
        return Lang.GUI.REFINING.translate().getString();
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(RefiningRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.stream(recipe.ingredient().getItems()).map(stack -> ItemHandlerHelper.copyStackWithSize(stack, recipe.count())).collect(Collectors.toList())));
        ingredients.setOutput(VanillaTypes.ITEM, recipe.result());
    }

    @Override
    public void setRecipe(IRecipeLayout layout, RefiningRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup stacks = layout.getItemStacks();
        stacks.init(0, true, 0, 0);
        stacks.setBackground(0, helper.getSlotDrawable());
        stacks.init(1, false, 60, 0);
        stacks.setBackground(1, helper.getSlotDrawable());
        stacks.set(ingredients);
    }

    @Override
    public void draw(RefiningRecipe recipe, MatrixStack poseStack, double mouseX, double mouseY) {
        emptyArrow.draw(poseStack, 24, 1);
        IDrawableAnimated arrow = arrows.computeIfAbsent(recipe.time(), time -> helper.createAnimatedDrawable(filledArrow, time, IDrawableAnimated.StartDirection.LEFT, false));
        arrow.draw(poseStack, 24, 1);
        FontRenderer font = Minecraft.getInstance().font;
        ITextComponent text = Lang.GUI.EMC_REFINERY_TIME.translate(EMCFormat.getComponent(recipe.emc()), recipe.time() / 20.0);
        font.draw(poseStack, text, (WIDTH - font.width(text)) / 2f, 23, 0x808080);
    }
}
