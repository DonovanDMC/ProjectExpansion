package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArcaneCraftingTransferHandler implements IRecipeTransferHandler<ContainerArcaneTransmutationTablet, RecipeHolder<CraftingRecipe>> {
    public static final int BLUE_SLOT_HIGHLIGHT_COLOR = 1073742079;
    public static final int RED_SLOT_HIGHLIGHT_COLOR = 1727987712;
    public static final int BLUE_PLUS_BUTTON_COLOR = -2142943745;
    public static final int ORANGE_PLUS_BUTTON_COLOR = -2130729728;

    @Override
    public Class<ContainerArcaneTransmutationTablet> getContainerClass() {
        return ContainerArcaneTransmutationTablet.class;
    }

    @Override
    public Optional<MenuType<ContainerArcaneTransmutationTablet>> getMenuType() {
        return MenuTypes.ARCANE_TRANSMUTATION_TABLET.asOptional();
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    private List<Integer> findMissingSlots(IRecipeSlotsView recipe, ContainerArcaneTransmutationTablet container, Player player) {
        List<IRecipeSlotView> ingredients = recipe.getSlotViews();
        List<Integer> slots = new ArrayList<>();

        for(int i = 1; i < ingredients.size(); ++i) {
            ItemStack[] items = (ingredients.get(i)).getItemStacks().toArray(ItemStack[]::new);
            boolean found = false;
            if (ingredients.get(i).isEmpty()) {
                found = true;
            } else {
                for (ItemStack stack : items) {
                    if (player.getInventory().contains(stack) || container.getProvider().hasKnowledge(stack) && IEMCProxy.INSTANCE.getValue(stack) <= container.transmutationInventory.getAvailableEmcAsLong()) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                slots.add(i - 1);
            }
        }

        return slots;
    }

    public @Nullable IRecipeTransferError transferRecipe(ContainerArcaneTransmutationTablet container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView iRecipeSlotsView, Player player, boolean transferAll, boolean doTransfer) {
        if (doTransfer) {
            List<List<ItemStack>> itemStack = new ArrayList<>();
            List<ItemStack> emptyStack = new ArrayList<>();
            emptyStack.add(ItemStack.EMPTY);

            for(int i = 1; i < iRecipeSlotsView.getSlotViews().size(); ++i) {
                if (iRecipeSlotsView.getSlotViews().get(i).getItemStacks().toList().isEmpty()) {
                    itemStack.add(emptyStack);
                } else {
                    itemStack.add(iRecipeSlotsView.getSlotViews().get(i).getItemStacks().toList());
                }
            }

            PacketDistributor.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(itemStack, transferAll, Optional.of(recipe.id())));
            return null;
        } else {
            List<Integer> missing = this.findMissingSlots(iRecipeSlotsView, container, player);
            boolean plus = !missing.isEmpty();
            int color = plus ? ORANGE_PLUS_BUTTON_COLOR : BLUE_PLUS_BUTTON_COLOR;
            return new ErrorRenderer(iRecipeSlotsView, missing, color);
        }
    }

    private record ErrorRenderer(IRecipeSlotsView iRecipeSlotsView, List<Integer> missing, int color) implements IRecipeTransferError {
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
                if (this.missing.contains(i)) {
                    this.iRecipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT).get(i).drawHighlight(guiGraphics, RED_SLOT_HIGHLIGHT_COLOR);
                }
            }

            poseStack.popPose();
        }
    }
}
