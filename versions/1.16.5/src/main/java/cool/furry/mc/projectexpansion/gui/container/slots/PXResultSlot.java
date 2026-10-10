package cool.furry.mc.projectexpansion.gui.container.slots;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.gui.container.inventory.PXCraftingContainer;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.ForgeHooks;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PXResultSlot extends CraftingResultSlot {
    private final PXCraftingContainer craftSlots;
    private final PlayerEntity player;
    private final ContainerArcaneTransmutationTablet tablet;
    public PXResultSlot(PlayerEntity player, PXCraftingContainer craftSlots, CraftResultInventory container, ContainerArcaneTransmutationTablet tablet, int slot, int xPosition, int yPosition) {
        super(player, craftSlots, container, slot, xPosition, yPosition);
        this.craftSlots = craftSlots;
        this.player = player;
        this.tablet = tablet;
    }

    @Override
    public ItemStack onTake(PlayerEntity player, ItemStack stack) {
        learnResult(stack);
        List<ItemStack> previousItems = craftSlots.getItems().stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));
        takeIngredients(player, stack);
        refillAfterTake(previousItems);
        return stack;
    }

    // same as CraftingResultSlot#onTake, but the leftovers come from the recipe that was used instead of the first one that matches
    private void takeIngredients(PlayerEntity player, ItemStack stack) {
        checkTakeAchievements(stack);
        ForgeHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remaining = tablet.getRemainingItems(craftSlots);
        ForgeHooks.setCraftingPlayer(null);

        for (int slot = 0; slot < remaining.size(); slot++) {
            ItemStack current = craftSlots.getItem(slot);
            ItemStack leftover = remaining.get(slot);
            if (!current.isEmpty()) {
                tablet.payForTaken(slot, 1);
                craftSlots.removeItem(slot, 1);
                current = craftSlots.getItem(slot);
            }

            if (!leftover.isEmpty()) {
                if (current.isEmpty()) {
                    craftSlots.setItem(slot, leftover);
                } else if (Util.isSameItemSameTags(current, leftover)) {
                    leftover.grow(current.getCount());
                    craftSlots.setItem(slot, leftover);
                } else if (!this.player.inventory.add(leftover)) {
                    this.player.drop(leftover, false);
                }
            }
        }
    }

    @Override
    public boolean mayPickup(PlayerEntity player) {
        return super.mayPickup(player) && tablet.canAffordCraft();
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        learnResult(stack);
        super.onQuickCraft(stack, amount);
    }

    protected void learnResult(ItemStack stack) {
        if (player instanceof ServerPlayerEntity && ProjectEAPI.getEMCProxy().hasValue(stack)) {
            tablet.transmutationInventory.handleKnowledge(stack);
        }
    }

    protected void refillAfterTake(List<ItemStack> items) {
        if (player.level.isClientSide || tablet.skipRefill) return;
        List<ItemStack> currentItems = craftSlots.getItems().stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < currentItems.size(); i++) {
            ItemStack stack = currentItems.get(i);
            // if empty or same as was originally present, skip
            if (stack.isEmpty() || Util.isSameItemSameTags(items.get(i), stack)) continue;
            // if it has a value, move to tablet
            if (Util.canConvertWithoutLoss(stack)) {
                tablet.transmutationInventory.handleKnowledge(stack);
                long value = ProjectEAPI.getEMCProxy().getValue(stack);
                tablet.transmutationInventory.addEmc(BigInteger.valueOf(value).multiply(BigInteger.valueOf(stack.getCount())));
                currentItems.set(i, ItemStack.EMPTY);
                craftSlots.setItem(i, ItemStack.EMPTY);
                continue;
            }
            // else, attempt to insert into the inventory
            ItemStack result = Util.returnToInventory(tablet.getPlayer().inventory, player, stack, false);
            // if we still have leftovers, just leave it in the slot. We'll skip over the slot later
            currentItems.set(i, result);
            craftSlots.setItem(i, result);
        }

        List<List<ItemStack>> recipe = items.stream().map(Collections::singletonList).map(ArrayList::new).collect(Collectors.toList());
        tablet.fillCraftingSlots(recipe, false);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        tablet.isCrafting = !stack.isEmpty(); // used for the arrow
    }
}
