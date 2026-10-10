package cool.furry.mc.projectexpansion.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.recipe.RefiningRecipe;
import cool.furry.mc.projectexpansion.registries.Items;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RefiningCategory implements IRecipeCategory<RefiningRecipe> {
    public static final RecipeType<RefiningRecipe> TYPE = new RecipeType<>(Main.rl("refining"), RefiningRecipe.class);
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
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.EMC_REFINERY.get()));
        this.emptyArrow = helper.createDrawable(TEXTURE, 79, 34, 24, 17);
        this.filledArrow = helper.createDrawable(TEXTURE, 176, 0, 24, 17);
    }

    @Override
    public RecipeType<RefiningRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getUid() {
        return TYPE.getUid();
    }

    @Override
    @SuppressWarnings("removal")
    public Class<? extends RefiningRecipe> getRecipeClass() {
        return RefiningRecipe.class;
    }

    @Override
    public Component getTitle() {
        return Lang.GUI.REFINING.translate();
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
    public void setRecipe(IRecipeLayoutBuilder builder, RefiningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).setBackground(helper.getSlotDrawable(), -1, -1)
                .addItemStacks(Arrays.stream(recipe.ingredient().getItems()).map(stack -> Platform.copyWithCount(stack, recipe.count())).toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 1).setBackground(helper.getSlotDrawable(), -1, -1).addItemStack(recipe.result());
    }

    @Override
    public void draw(RefiningRecipe recipe, IRecipeSlotsView slots, PoseStack poseStack, double mouseX, double mouseY) {
        emptyArrow.draw(poseStack, 24, 1);
        IDrawableAnimated arrow = arrows.computeIfAbsent(recipe.time(), time -> helper.createAnimatedDrawable(filledArrow, time, IDrawableAnimated.StartDirection.LEFT, false));
        arrow.draw(poseStack, 24, 1);
        Font font = Minecraft.getInstance().font;
        Component text = Lang.GUI.EMC_REFINERY_TIME.translate(EMCFormat.getComponent(recipe.emc()), recipe.time() / 20.0);
        font.draw(poseStack, text, (WIDTH - font.width(text)) / 2f, 23, 0x808080);
    }
}
