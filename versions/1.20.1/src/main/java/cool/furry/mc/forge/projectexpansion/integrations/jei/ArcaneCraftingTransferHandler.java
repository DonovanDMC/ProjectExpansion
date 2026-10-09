package cool.furry.mc.forge.projectexpansion.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.gui.container.RecipeTransferPlan;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import cool.furry.mc.forge.projectexpansion.registries.MenuTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArcaneCraftingTransferHandler implements IRecipeTransferHandler<ContainerArcaneTransmutationTablet, CraftingRecipe> {
    public static final int BLUE_SLOT_HIGHLIGHT_COLOR = 1073742079;
    public static final int RED_SLOT_HIGHLIGHT_COLOR = 1727987712;

    @Override
    public Class<ContainerArcaneTransmutationTablet> getContainerClass() {
        return ContainerArcaneTransmutationTablet.class;
    }

    @Override
    public Optional<MenuType<ContainerArcaneTransmutationTablet>> getMenuType() {
        return Optional.of(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    private List<List<ItemStack>> getRecipeStacks(IRecipeSlotsView recipeSlots) {
        List<List<ItemStack>> stacks = new ArrayList<>();
        // the first slot is the output
        for (int i = 1; i < recipeSlots.getSlotViews().size(); ++i) {
            List<ItemStack> items = recipeSlots.getSlotViews().get(i).getItemStacks().toList();
            stacks.add(items.isEmpty() ? List.of(ItemStack.EMPTY) : items);
        }
        return stacks;
    }

    public @Nullable IRecipeTransferError transferRecipe(ContainerArcaneTransmutationTablet container, CraftingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, Player player, boolean transferAll, boolean doTransfer) {
        List<List<ItemStack>> stacks = getRecipeStacks(iRecipeSlotsView);
        if (doTransfer) {
            PacketHandler.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(stacks, transferAll, Optional.of(recipe.getId())));
            return null;
        }
        return new ErrorRenderer(iRecipeSlotsView, container.planRecipeTransfer(stacks));
    }

    private record ErrorRenderer(IRecipeSlotsView iRecipeSlotsView, RecipeTransferPlan plan) implements IRecipeTransferError {
        public IRecipeTransferError.Type getType() {
            return Type.COSMETIC;
        }

        public int getButtonHighlightColor() {
            return 0;
        }

        public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView slots, int recipeX, int recipeY) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate((float)recipeX, (float)recipeY, 0.0F);

            for(int i = 0; i < this.iRecipeSlotsView.getSlotViews().size(); ++i) {
                if (plan.excludedSlots().contains(i)) {
                    this.iRecipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT).get(i).drawHighlight(guiGraphics, RED_SLOT_HIGHLIGHT_COLOR);
                }
            }

            poseStack.popPose();
            // this version of jei has no tooltip hook for transfer errors, so draw it like jei's own errors do
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, plan.getTooltip(), mouseX, mouseY);
        }
    }
}
