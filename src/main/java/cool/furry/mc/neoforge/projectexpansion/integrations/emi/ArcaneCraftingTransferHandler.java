package cool.furry.mc.neoforge.projectexpansion.integrations.emi;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

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
        return recipe.getBackingRecipe() != null && recipe.getBackingRecipe().value() instanceof CraftingRecipe;
    }

    private EmiPlayerInventory buildEmcAwareInventory(EmiRecipe recipe, ContainerArcaneTransmutationTablet container) {
        List<EmiStack> stacks = new ArrayList<>();
        for (Slot slot : getInputSources(container)) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) stacks.add(EmiStack.of(item));
        }
        long availableEmc = container.transmutationInventory.getAvailableEmcAsLong();
        for (EmiIngredient ingredient : recipe.getInputs()) {
            for (EmiStack emiStack : ingredient.getEmiStacks()) {
                ItemStack item = emiStack.getItemStack();
                if (item.isEmpty()) continue;
                long value = IEMCProxy.INSTANCE.getValue(item);
                if (value > 0 && value <= availableEmc && container.getProvider().hasKnowledge(item)) {
                    stacks.add(EmiStack.of(item.copyWithCount((int) Math.min(64, availableEmc / value))));
                }
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        return buildEmcAwareInventory(recipe, context.getScreenHandler()).canCraft(recipe);
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context, List<Widget> widgets, GuiGraphics graphics) {
        StandardRecipeHandler.renderMissing(recipe, buildEmcAwareInventory(recipe, context.getScreenHandler()), widgets, graphics);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneTransmutationTablet> context) {
        List<List<ItemStack>> itemStacks = new ArrayList<>();
        List<ItemStack> emptyStack = List.of(ItemStack.EMPTY);

        for (EmiIngredient ingredient : recipe.getInputs()) {
            List<ItemStack> stacks = ingredient.getEmiStacks().stream()
                    .map(EmiStack::getItemStack)
                    .filter(s -> !s.isEmpty())
                    .toList();
            itemStacks.add(stacks.isEmpty() ? emptyStack : stacks);
        }

        boolean transferAll = context.getDestination() == EmiCraftContext.Destination.INVENTORY;
        PacketDistributor.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(itemStacks, transferAll));
        return true;
    }
}
