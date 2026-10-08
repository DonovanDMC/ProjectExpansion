package cool.furry.mc.neoforge.projectexpansion.gui.container.slots;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.common.CommonHooks;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PXResultSlot extends ResultSlot {
    private final CraftingContainer craftSlots;
    private final Player player;
    private final ContainerArcaneTransmutationTablet tablet;
    public PXResultSlot(Player player, CraftingContainer craftSlots, ResultContainer container, ContainerArcaneTransmutationTablet tablet, int slot, int xPosition, int yPosition) {
        super(player, craftSlots, container, slot, xPosition, yPosition);
        this.craftSlots = craftSlots;
        this.player = player;
        this.tablet = tablet;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        learnResult(stack);
        List<ItemStack> previousItems = craftSlots.getItems().stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));
        takeIngredients(player, stack);
        refillAfterTake(previousItems);
    }

    // same as ResultSlot#onTake, but the leftovers come from the recipe that was used instead of the first one that matches
    private void takeIngredients(Player player, ItemStack stack) {
        checkTakeAchievements(stack);
        CraftingInput.Positioned positioned = craftSlots.asPositionedCraftInput();
        CraftingInput input = positioned.input();
        int left = positioned.left();
        int top = positioned.top();
        CommonHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remaining = tablet.getRemainingItems(input);
        CommonHooks.setCraftingPlayer(null);

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int slot = x + left + (y + top) * craftSlots.getWidth();
                ItemStack current = craftSlots.getItem(slot);
                ItemStack leftover = remaining.get(x + y * input.width());
                if (!current.isEmpty()) {
                    tablet.payForTaken(slot, 1);
                    craftSlots.removeItem(slot, 1);
                    current = craftSlots.getItem(slot);
                }

                if (!leftover.isEmpty()) {
                    if (current.isEmpty()) {
                        craftSlots.setItem(slot, leftover);
                    } else if (ItemStack.isSameItemSameComponents(current, leftover)) {
                        leftover.grow(current.getCount());
                        craftSlots.setItem(slot, leftover);
                    } else if (!this.player.getInventory().add(leftover)) {
                        this.player.drop(leftover, false);
                    }
                }
            }
        }
    }

    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && tablet.canAffordCraft();
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        learnResult(stack);
        super.onQuickCraft(stack, amount);
    }

    protected void learnResult(ItemStack stack) {
        if (player instanceof ServerPlayer && IEMCProxy.INSTANCE.hasValue(stack)) {
            tablet.transmutationInventory.handleKnowledge(stack);
        }
    }

    protected void refillAfterTake(List<ItemStack> items) {
        if (player.level().isClientSide || tablet.skipRefill) return;
        List<ItemStack> currentItems = craftSlots.getItems().stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < currentItems.size(); i++) {
            ItemStack stack = currentItems.get(i);
            // if empty or same as was originally present, skip
            if (stack.isEmpty() || ItemStack.isSameItemSameComponents(items.get(i), stack)) continue;
            // if it has a value, move to tablet
            if (Util.canConvertWithoutLoss(stack)) {
                tablet.transmutationInventory.handleKnowledge(stack);
                long value = IEMCProxy.INSTANCE.getValue(stack);
                tablet.transmutationInventory.addEmc(BigInteger.valueOf(value).multiply(BigInteger.valueOf(stack.getCount())));
                currentItems.set(i, ItemStack.EMPTY);
                craftSlots.setItem(i, ItemStack.EMPTY);
                continue;
            }
            // else, attempt to insert into the inventory
            ItemStack result = Util.returnToInventory(tablet.getPlayer().getInventory(), player, stack, false);
            // if we still have leftovers, just leave it in the slot. We'll skip over the slot later
            currentItems.set(i, result);
            craftSlots.setItem(i, result);
        }

        List<List<ItemStack>> recipe = items.stream().map(List::of).map(ArrayList::new).collect(Collectors.toList());
        tablet.fillCraftingSlots(recipe, false);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        tablet.isCrafting = !stack.isEmpty(); // used for the arrow
    }
}
