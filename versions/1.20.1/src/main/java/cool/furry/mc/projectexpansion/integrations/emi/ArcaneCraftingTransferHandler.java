package cool.furry.mc.projectexpansion.integrations.emi;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.RecipeTransferPlan;
import cool.furry.mc.projectexpansion.net.PacketHandler;
import cool.furry.mc.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ArcaneCraftingTransferHandler implements StandardRecipeHandler<ContainerArcaneTransmutationTablet> {
    private static final int PLAYER = 27;
    private static final int PLAYER_COUNT = 36;
    private static final int CRAFTING = 64;
    private static final int CRAFTING_COUNT = 9;

    @Override
    public List<Slot> getInputSources(ContainerArcaneTransmutationTablet container) {
        return container.slots.subList(PLAYER, PLAYER + PLAYER_COUNT);
    }

    @Override
    public List<Slot> getCraftingSlots(ContainerArcaneTransmutationTablet container) {
        return container.slots.subList(CRAFTING, CRAFTING + CRAFTING_COUNT);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getBackingRecipe() != null && recipe.getBackingRecipe() instanceof CraftingRecipe;
    }

    private List<List<ItemStack>> getRecipeStacks(EmiRecipe recipe) {
        List<List<ItemStack>> itemStacks = new ArrayList<>();
        List<ItemStack> emptyStack = List.of(ItemStack.EMPTY);

        for (EmiIngredient ingredient : recipe.getInputs()) {
            List<ItemStack> stacks = ingredient.getEmiStacks().stream()
                    .map(EmiStack::getItemStack)
                    .filter(s -> !s.isEmpty())
                    .toList();
            itemStacks.add(stacks.isEmpty() ? emptyStack : stacks);
        }
        return itemStacks;
    }

    private RecipeTransferPlan plan(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        return context.getScreenHandler().planRecipeTransfer(getRecipeStacks(recipe));
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        return plan(recipe, context).isComplete();
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        return plan(recipe, context).getTooltip().stream()
                .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                .toList();
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context, List<Widget> widgets, GuiGraphics graphics) {
        RecipeTransferPlan plan = plan(recipe, context);
        // match widgets to recipe inputs by identity, the same way emi does
        Set<EmiIngredient> excluded = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i : plan.excludedSlots()) {
            if (i < recipe.getInputs().size()) excluded.add(recipe.getInputs().get(i));
        }
        for (Widget widget : widgets) {
            if (widget instanceof SlotWidget slot && slot.getRecipe() == null && excluded.contains(slot.getStack())) {
                Bounds bounds = slot.getBounds();
                graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0x44FF0000);
            }
        }
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        List<List<ItemStack>> itemStacks = getRecipeStacks(recipe);
        boolean transferAll = context.getDestination() == EmiCraftContext.Destination.INVENTORY;
        Optional<ResourceLocation> recipeId = Optional.ofNullable(recipe.getBackingRecipe()).map(Recipe::getId);
        PacketHandler.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(itemStacks, transferAll, recipeId));
        return true;
    }
}
