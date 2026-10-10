package cool.furry.mc.projectexpansion.integrations.jei;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import cool.furry.mc.projectexpansion.gui.GuiGraphics;
import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.RecipeTransferPlan;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArcaneCraftingTransferHandler implements IRecipeTransferHandler<ContainerArcaneTransmutationTablet> {
    public static final int RED_SLOT_HIGHLIGHT_COLOR = 1727987712;
    // jei's crafting layout puts the output in slot 0 and the 9 inputs after it
    private static final int INPUT_OFFSET = 1;
    private static final int INPUT_COUNT = 9;

    @Override
    public Class<ContainerArcaneTransmutationTablet> getContainerClass() {
        return ContainerArcaneTransmutationTablet.class;
    }

    private List<List<ItemStack>> getRecipeStacks(IRecipeLayout layout) {
        Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = layout.getItemStacks().getGuiIngredients();
        List<List<ItemStack>> stacks = new ArrayList<>();
        for (int i = 0; i < INPUT_COUNT; ++i) {
            IGuiIngredient<ItemStack> ingredient = ingredients.get(i + INPUT_OFFSET);
            List<ItemStack> items = new ArrayList<>();
            if (ingredient != null) {
                for (ItemStack stack : ingredient.getAllIngredients()) {
                    if (stack != null) items.add(stack);
                }
            }
            stacks.add(items.isEmpty() ? ImmutableList.of(ItemStack.EMPTY) : items);
        }
        return stacks;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(ContainerArcaneTransmutationTablet container, Object recipe, IRecipeLayout layout, PlayerEntity player, boolean transferAll, boolean doTransfer) {
        if (!(recipe instanceof ICraftingRecipe)) return null;
        List<List<ItemStack>> stacks = getRecipeStacks(layout);
        if (doTransfer) {
            PacketHandler.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(stacks, transferAll, Optional.of(((ICraftingRecipe) recipe).getId())));
            return null;
        }
        return new ErrorRenderer(container.planRecipeTransfer(stacks));
    }

    private static final class ErrorRenderer implements IRecipeTransferError {
        private final RecipeTransferPlan plan;

        public ErrorRenderer(RecipeTransferPlan plan) {
            this.plan = plan;
        }

        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public void showError(MatrixStack pose, int mouseX, int mouseY, IRecipeLayout layout, int recipeX, int recipeY) {
            Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = layout.getItemStacks().getGuiIngredients();
            for (int i : plan.excludedSlots()) {
                IGuiIngredient<ItemStack> ingredient = ingredients.get(i + INPUT_OFFSET);
                if (ingredient != null) ingredient.drawHighlight(pose, RED_SLOT_HIGHLIGHT_COLOR, recipeX, recipeY);
            }

            // this version of jei has no tooltip hook for transfer errors, so draw it like jei's own errors do
            new GuiGraphics(pose, Minecraft.getInstance().screen).renderComponentTooltip(Minecraft.getInstance().font, plan.getTooltip(), mouseX, mouseY);
        }
    }
}
