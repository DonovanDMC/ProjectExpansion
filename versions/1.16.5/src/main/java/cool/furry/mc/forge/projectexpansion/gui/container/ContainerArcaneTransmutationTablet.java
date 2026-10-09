package cool.furry.mc.forge.projectexpansion.gui.container;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import cool.furry.mc.forge.projectexpansion.gui.container.inventory.PXCraftingContainer;
import cool.furry.mc.forge.projectexpansion.gui.container.slots.PXCraftingSlot;
import cool.furry.mc.forge.projectexpansion.gui.container.slots.PXInputSlot;
import cool.furry.mc.forge.projectexpansion.gui.container.slots.PXOutputSlot;
import cool.furry.mc.forge.projectexpansion.gui.container.slots.PXResultSlot;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import cool.furry.mc.forge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.forge.projectexpansion.util.ContainerData;
import cool.furry.mc.forge.projectexpansion.util.EmcLock;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.gameObjs.container.PEHandContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.*;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerArcaneTransmutationTablet extends PEHandContainer {
    private static final int[] ROTATION_SLOTS = {0, 1, 2, 5, 8, 7, 6, 3};
    private final List<SlotInput> inputSlots = new ArrayList<>();
    private final List<PXOutputSlot> outputSlots = new ArrayList<>();
    public final TransmutationInventory transmutationInventory;
    private PXCraftingContainer craftSlots;
    private CraftResultInventory resultSlots;
    private SlotUnlearn unlearn;
    private static final int INPUT = 0;
    private static final int INPUT_COUNT = 4;
    private static final int LOCK = 8;
    private static final int CONSUME = 9;
    private static final int UNLEARN = 10;
    private static final int OUTPUT = 11;
    private static final int OUTPUT_COUNT = 16;
    private static final int PLAYER = 27;
    private static final int PLAYER_COUNT = 36;
    private static final int CRAFTING = 64;
    private static final int RESULT = 63;
    private static final int CRAFTING_COUNT = 9;
    public int lockX, lockY, consumeX, consumeY, unlearnX, unlearnY;
    private final PlayerInventory playerInv;
    private final PlayerEntity player;
    private final IKnowledgeProvider provider;
    public boolean isCrafting = false;
    public boolean skipRefill = false;
    // the recipe the player picked when more than one matches, only used on the server
    @Nullable private ResourceLocation selectedRecipe;
    @Nullable private ICraftingRecipe currentRecipe;
    // how many items in each crafting slot came from transmutation and haven't been paid for yet
    // they're only paid for when used in a craft or taken out, so a full grid left sitting doesn't hold onto emc
    private final int[] ghostCounts = new int[CRAFTING_COUNT];

    public static ContainerArcaneTransmutationTablet fromNetwork(int windowId, PlayerInventory playerInv, PacketBuffer buf) {
        @Nullable IKnowledgeProvider provider = playerInv.player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (provider == null) throw new NullPointerException("provider is null");
        ContainerData data = ContainerData.decode(buf);
        return new ContainerArcaneTransmutationTablet(windowId, playerInv, provider, data.hand().orElse(null), data.selected());
    }

    public ContainerArcaneTransmutationTablet(int windowId, PlayerInventory playerInv, IKnowledgeProvider provider) {
        this(windowId, playerInv, provider, null, 0);
    }

    public ContainerArcaneTransmutationTablet(int windowId, PlayerInventory playerInv, IKnowledgeProvider provider, @Nullable Hand hand, int selected) {
        super(new ContainerTypeRegistryObject<>(MenuTypes.ARCANE_TRANSMUTATION_TABLET), windowId, hand, selected);
        this.playerInv = playerInv;
        this.player = playerInv.player;
        this.provider = provider;
        this.transmutationInventory = new TransmutationInventory(playerInv.player);
        initSlots();
    }

    private void initSlots() {
        addSlot(new PXInputSlot(transmutationInventory, INPUT, 30, 21));
        addSlot(new PXInputSlot(transmutationInventory, 1, 130,21));
        addSlot(new PXInputSlot(transmutationInventory, 2, 8, 43));
        addSlot(new PXInputSlot(transmutationInventory, 3, 152, 43));
        addSlot(new PXInputSlot(transmutationInventory, 4, 8, 94));
        addSlot(new PXInputSlot(transmutationInventory, 5, 152, 94));
        addSlot(new PXInputSlot(transmutationInventory, 6, 30, 115));
        addSlot(new PXInputSlot(transmutationInventory, 7, 130, 115));
        addSlot(new SlotLock(transmutationInventory, LOCK, lockX = 80, lockY = 68));
        addSlot(new SlotConsume(transmutationInventory, CONSUME, consumeX = 152, consumeY = 115));
        addSlot(unlearn = new SlotUnlearn(transmutationInventory, UNLEARN, unlearnX = 8, unlearnY = 115));
        addSlot(new PXOutputSlot(transmutationInventory, OUTPUT, 80, 20));
        addSlot(new PXOutputSlot(transmutationInventory, 12, 105, 26));
        addSlot(new PXOutputSlot(transmutationInventory, 13, 55, 26));
        addSlot(new PXOutputSlot(transmutationInventory, 14, 123, 44));
        addSlot(new PXOutputSlot(transmutationInventory, 15, 37, 44));
        addSlot(new PXOutputSlot(transmutationInventory, 16, 128, 68));
        addSlot(new PXOutputSlot(transmutationInventory, 17, 32, 68));
        addSlot(new PXOutputSlot(transmutationInventory, 18, 123, 92));
        addSlot(new PXOutputSlot(transmutationInventory, 19, 37, 92));
        addSlot(new PXOutputSlot(transmutationInventory, 20, 105, 110));
        addSlot(new PXOutputSlot(transmutationInventory, 21, 55, 110));
        addSlot(new PXOutputSlot(transmutationInventory, 22, 80, 116));
        addSlot(new PXOutputSlot(transmutationInventory, 23, 60, 48));
        addSlot(new PXOutputSlot(transmutationInventory, 24, 100, 48));
        addSlot(new PXOutputSlot(transmutationInventory, 25, 60, 88));
        addSlot(new PXOutputSlot(transmutationInventory, 26, 100, 88));
        addPlayerInventory(playerInv, 8, 135);
        this.craftSlots = new PXCraftingContainer(this, 3, 3);
        this.resultSlots = new CraftResultInventory();

        addSlot(new PXResultSlot(playerInv.player, this.craftSlots, this.resultSlots, this,0, -23, 75));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new PXCraftingSlot(this, this.craftSlots, j + i * 3, -59 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < CRAFTING_COUNT; i++) {
            addDataSlot(IntReferenceHolder.shared(ghostCounts, i));
        }

    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (slot instanceof PXInputSlot) {
            PXInputSlot inputSlot = ((PXInputSlot) slot);
            this.inputSlots.add(inputSlot);
        } else if (slot instanceof PXOutputSlot) {
            PXOutputSlot outputSlot = ((PXOutputSlot) slot);
            this.outputSlots.add(outputSlot);
        }
        return super.addSlot(slot);
    }

    @Override
    public void removed(PlayerEntity player) {
        super.removed(player);
        discardGhosts();
        if (!player.isAlive() || player instanceof ServerPlayerEntity && ((ServerPlayerEntity) player).hasDisconnected()) {
            player.drop(unlearn.getItem(), false);
            for (ItemStack stack : this.craftSlots.getItems()) {
                player.drop(stack, false);
            }
        } else {
            Util.returnToInventoryOrTransmutation(playerInv, player, provider, unlearn.getItem(), false, true);
            for (ItemStack stack : this.craftSlots.getItems()) {
                Util.returnToInventoryOrTransmutation(playerInv, player, provider, stack, false, true);
            }
        }
        // the result slot is only a preview of the recipe output, so it isn't returned
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int slotIndex) {
        if (this.tryGetSlot(slotIndex) instanceof PXCraftingSlot && ((PXCraftingSlot) this.tryGetSlot(slotIndex)).hasItem()) {
            PXCraftingSlot slot = (PXCraftingSlot) this.tryGetSlot(slotIndex);
            if (!slot.mayPickup(player)) return ItemStack.EMPTY;
            ItemStack itemStack = slot.getItem().copy();
            int count = itemStack.getCount();
            this.moveItemStackTo(itemStack, PLAYER, (PLAYER + PLAYER_COUNT - 1), true);
            // only remove what actually fit, remove also pays for any ghost items that left
            int moved = count - itemStack.getCount();
            if (moved > 0) slot.remove(moved);
            return ItemStack.EMPTY;
        }

        if (this.tryGetSlot(slotIndex) instanceof PXResultSlot || slotIndex == RESULT) {
            Slot slot = this.tryGetSlot(slotIndex);
            // shift clicking crafts repeatedly, so check every time that the ghost items can still be paid for
            if (slot != null && slot.hasItem() && slot.mayPickup(player)) {
                ItemStack itemStack1 = slot.getItem();
                ItemStack itemStack = itemStack1.copy();
                itemStack1.getItem().onCraftedBy(itemStack, player.level, player);
                if (!this.moveItemStackTo(itemStack1, PLAYER, (PLAYER + PLAYER_COUNT - 1), true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack1, itemStack);
                if (itemStack1.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (itemStack1.getCount() == itemStack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.onTake(player, itemStack1);
                if (slotIndex == 0) {
                    player.drop(itemStack1, false);
                }

                return itemStack;
            }
        }

        if ((slotIndex >= INPUT && slotIndex < (INPUT + INPUT_COUNT)) || slotIndex == LOCK || slotIndex == UNLEARN) {
            //Input Slots, lock slot, and unlearn slot, defer to super (allow basic sneak clicking out of container)
            return super.quickMoveStack(player, slotIndex);
        }

        Slot currentSlot = tryGetSlot(slotIndex);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= OUTPUT && slotIndex < (OUTPUT + OUTPUT_COUNT)) {
            ItemStack stack = currentSlot.getItem().copy();
            // Output Slots
            long itemEmc = ProjectEAPI.getEMCProxy().getValue(stack);
            //Double-check the item actually has Emc and something didn't just go terribly wrong
            if (itemEmc > 0) {
                //Note: We can just set the size here as newStack is a copy stack used for modifications
                stack.setCount(stack.getMaxStackSize());
                //Check how much we can fit of the stack
                int itemsRoomFor = stack.getCount() - ItemHelper.simulateFit(player.inventory.items, stack);
                if (itemsRoomFor == 1) {
                    BigInteger availableEMC = transmutationInventory.getAvailableEmc();
                    if (BigInteger.valueOf(itemEmc).compareTo(availableEMC) > 0) {
                        //We need more EMC than we have available, but we were only trying to get a single item.
                        // This means we can't actually produce any of the item
                        return ItemStack.EMPTY;
                    }
                    if (transmutationInventory.isServer()) {
                        transmutationInventory.removeEmc(BigInteger.valueOf(itemEmc));
                    }
                    stack.setCount(1);
                    ItemHandlerHelper.insertItemStacked(player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(NullPointerException::new), stack, false);
                } else if (itemsRoomFor > 1) {
                    BigInteger availableEMC = transmutationInventory.getAvailableEmc();
                    BigInteger emc = BigInteger.valueOf(itemEmc);
                    BigInteger totalEmc = emc.multiply(BigInteger.valueOf(itemsRoomFor));
                    if (totalEmc.compareTo(availableEMC) > 0) {
                        //We need more EMC than we have available, so we have to calculate how much we actually can produce
                        //Note: We first multiply then compare, as the larger the numbers are the less efficient division becomes
                        BigInteger numOperations = availableEMC.divide(emc);
                        //Note: As we already compared to a multiplication of an int times the number we divided by, so it should fit into an int
                        itemsRoomFor = numOperations.intValue();
                        totalEmc = emc.multiply(numOperations);
                        if (itemsRoomFor <= 0) {
                            return ItemStack.EMPTY;
                        }
                    }
                    if (transmutationInventory.isServer()) {
                        transmutationInventory.removeEmc(totalEmc);
                    }
                    //Set the stack size to what we found the max value is we have room for (capped at the stack's own max size)
                    stack.setCount(itemsRoomFor);
                    ItemHandlerHelper.insertItemStacked(player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(NullPointerException::new), stack, false);
                }
            }
        } else if (slotIndex >= (OUTPUT + OUTPUT_COUNT) && slotIndex < CRAFTING) {
            ItemStack slotStack = currentSlot.getItem();
            if (EmcLock.isLocked(slotStack)) return ItemStack.EMPTY;
            ItemStack stackToInsert = slotStack;
            if (stackToInsert.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
                //We are in the main inventory or the hot bar and are handling an item that can store EMC
                //Start by trying to insert it into the input slots, first attempting to stack with other items
                stackToInsert = insertItem(inputSlots, stackToInsert, true);
                if (slotStack.getCount() == stackToInsert.getCount()) {
                    //Then as long as if we still have the same number of items (failed to insert), try to insert it into the input slots allowing for empty items
                    stackToInsert = insertItem(inputSlots, stackToInsert, false);
                }
                if (slotStack.getCount() != stackToInsert.getCount()) {
                    return transferSuccess(currentSlot, player, slotStack, stackToInsert);
                }
            }
            //Else if we failed to do that also, transfer to the learn slot if the item has EMC
            long emc = ProjectEAPI.getEMCProxy().getSellValue(stackToInsert);
            if (emc > 0 || stackToInsert.getItem() instanceof Tome) {
                if (transmutationInventory.isServer()) {
                    BigInteger emcBigInt = BigInteger.valueOf(emc);
                    transmutationInventory.handleKnowledge(stackToInsert);
                    transmutationInventory.addEmc(emcBigInt.multiply(BigInteger.valueOf(stackToInsert.getCount())));
                }
                currentSlot.set(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack clickPostValidate(int slotIndex, int dragType, ClickType clickType, PlayerEntity player) {
        if (player.level.isClientSide && transmutationInventory.getHandlerForSlot(slotIndex) == transmutationInventory.outputs && tryGetSlot(slotIndex) != null) {
            Slot slot = Objects.requireNonNull(tryGetSlot(slotIndex));
            moze_intel.projecte.network.PacketHandler.sendToServer(new SearchUpdatePKT(transmutationInventory.getIndexFromSlot(slotIndex), slot.getItem()));
        }
        return super.clickPostValidate(slotIndex, dragType, clickType, player);
    }

    private ItemStack transferSuccess(Slot currentSlot, PlayerEntity player, ItemStack slotStack, ItemStack stackToInsert) {
        int difference = slotStack.getCount() - stackToInsert.getCount();
        ItemStack newStack = currentSlot.remove(difference);
        currentSlot.onTake(player, newStack);
        return newStack;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof SlotConsume || slot instanceof SlotUnlearn || slot instanceof SlotInput || slot instanceof SlotLock || slot instanceof SlotOutput);
    }

    @Override
    public ItemStack clicked(int slotId, int dragType, ClickType clickType, PlayerEntity player) {
        if (clickType == ClickType.QUICK_MOVE) skipRefill = true;
        ItemStack result = super.clicked(slotId, dragType, clickType, player);
        if (clickType == ClickType.QUICK_MOVE) skipRefill = false;
        return result;
    }

    @Override
    public void slotsChanged(IInventory container) {
        for (int i = 0; i < CRAFTING_COUNT; i++) {
            ghostCounts[i] = Math.min(ghostCounts[i], craftSlots.getItem(i).getCount());
        }
        slotChangedCraftingGrid(this, this.player.level, this.player, this.craftSlots, this.resultSlots);
        super.slotsChanged(container);
    }

    protected static void slotChangedCraftingGrid(ContainerArcaneTransmutationTablet menu, World level, PlayerEntity player, CraftingInventory crafting, CraftResultInventory result) {
        if (!level.isClientSide) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            ItemStack stack = ItemStack.EMPTY;
            RecipeManager recipeManager = Objects.requireNonNull(level.getServer()).getRecipeManager();
            // use the selected recipe if it still matches, otherwise whatever vanilla would pick
            Optional<ICraftingRecipe> optional = recipeManager.getRecipesFor(IRecipeType.CRAFTING, crafting, level).stream()
                    .filter(recipe -> recipe.getId().equals(menu.selectedRecipe))
                    .findFirst();
            if (!optional.isPresent()) optional = recipeManager.getRecipeFor(IRecipeType.CRAFTING, crafting, level);
            menu.currentRecipe = null;
            if (optional.isPresent()) {
                ICraftingRecipe recipe = optional.get();
                if (result.setRecipeUsed(level, serverPlayer, recipe)) {
                    stack = recipe.assemble(crafting);
                    menu.currentRecipe = recipe;
                }
            }

            result.setItem(0, stack);
            serverPlayer.connection.send(new SSetSlotPacket(menu.containerId, RESULT, stack));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // don't let double clicking quietly buy ghost items out of the grid
        if (slot instanceof PXCraftingSlot && ghostCounts[slot.getSlotIndex()] > 0) return false;
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public void onRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll, Optional<ResourceLocation> recipeId) {
        recipeId.ifPresent(id -> selectedRecipe = id);
        clearCrafting(false);
        fillCraftingSlots(recipe, transferAll);
    }

    /** @apiNote only call on server */
    public void selectRecipe(ResourceLocation id) {
        selectedRecipe = id;
        slotChangedCraftingGrid(this, this.player.level, this.player, this.craftSlots, this.resultSlots);
    }

    public PXCraftingContainer getCraftingInput() {
        return craftSlots;
    }

    public List<ICraftingRecipe> getMatchingRecipes() {
        World level = player.level;
        return level.getRecipeManager().getRecipesFor(IRecipeType.CRAFTING, getCraftingInput(), level);
    }

    public ItemStack getResultItem() {
        return resultSlots.getItem(0);
    }

    public int getGhostCount(int slot) {
        return ghostCounts[slot];
    }

    private static BigInteger getValue(ItemStack stack, int count) {
        return BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(stack)).multiply(BigInteger.valueOf(count));
    }

    public BigInteger getGhostCost(int slot) {
        return getValue(craftSlots.getItem(slot), ghostCounts[slot]);
    }

    // the emc that the ghost items in the grid will need
    private BigInteger getReservedEmc() {
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < CRAFTING_COUNT; i++) total = total.add(getGhostCost(i));
        return total;
    }

    // real items are always used first, so ghost items only leave once the real ones run out
    private int getGhostsTaken(int slot, int amount) {
        int real = craftSlots.getItem(slot).getCount() - ghostCounts[slot];
        return MathHelper.clamp(amount - real, 0, ghostCounts[slot]);
    }

    public boolean canAffordGhosts(int slot) {
        return provider.getEmc().compareTo(getGhostCost(slot)) >= 0;
    }

    // a craft takes one item from every filled slot
    public boolean canAffordCraft() {
        BigInteger cost = BigInteger.ZERO;
        for (int i = 0; i < CRAFTING_COUNT; i++) {
            if (!craftSlots.getItem(i).isEmpty()) cost = cost.add(getValue(craftSlots.getItem(i), getGhostsTaken(i, 1)));
        }
        return provider.getEmc().compareTo(cost) >= 0;
    }

    /** pays for any ghost items in the amount about to leave the slot, call before they're removed */
    public void payForTaken(int slot, int amount) {
        int ghosts = getGhostsTaken(slot, amount);
        if (ghosts <= 0 || !(player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
        provider.setEmc(provider.getEmc().subtract(getValue(craftSlots.getItem(slot), ghosts)).max(BigInteger.ZERO));
        provider.syncEmc(serverPlayer);
        ghostCounts[slot] -= ghosts;
    }

    private void discardGhosts() {
        for (int i = 0; i < CRAFTING_COUNT; i++) {
            if (ghostCounts[i] <= 0) continue;
            craftSlots.getItem(i).shrink(ghostCounts[i]);
            ghostCounts[i] = 0;
        }
    }

    // balancing and spreading move items between slots, so share the ghost items back out per item afterwards
    private void keepGhosts(Runnable action) {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (int i = 0; i < CRAFTING_COUNT; i++) {
            if (ghostCounts[i] <= 0) continue;
            ItemStack stack = craftSlots.getItem(i);
            int index = indexOfItem(items, stack);
            if (index == -1) {
                items.add(ItemHandlerHelper.copyStackWithSize(stack, 1));
                totals.add(ghostCounts[i]);
            } else {
                totals.set(index, totals.get(index) + ghostCounts[i]);
            }
        }

        action.run();

        for (int i = 0; i < CRAFTING_COUNT; i++) {
            ItemStack stack = craftSlots.getItem(i);
            int index = stack.isEmpty() ? -1 : indexOfItem(items, stack);
            ghostCounts[i] = index == -1 ? 0 : Math.min(stack.getCount(), totals.get(index));
            if (index != -1) totals.set(index, totals.get(index) - ghostCounts[i]);
        }
    }

    private static int indexOfItem(List<ItemStack> items, ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (Util.isSameItemSameTags(items.get(i), stack)) return i;
        }
        return -1;
    }

    // leftovers need to come from the recipe that was actually used, not just the first one that matches
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory input) {
        World level = player.level;
        if (currentRecipe != null && currentRecipe.matches(input, level)) {
            return currentRecipe.getRemainingItems(input);
        }
        return level.getRecipeManager().getRemainingItemsFor(IRecipeType.CRAFTING, input, level);
    }

    public void fillCraftingSlots(List<List<ItemStack>> recipe, boolean transferAll) {
        int max = Math.min(recipe.size(), craftSlots.getContainerSize());
        transferItems(recipe, max);

        if (transferAll) {
            for (int i = 0; i < 63; i++) {
                transferItems(recipe, max);
            }
        }

        slotChangedCraftingGrid(this, this.player.level, this.player, this.craftSlots, this.resultSlots);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean transferFromTablet(int i, List<ItemStack> possibilities) {
        if (!possibilities.isEmpty()) {
            possibilities.sort(Comparator.comparingLong(ProjectEAPI.getEMCProxy()::getValue));
        }

        for (ItemStack stack : possibilities) {
            ItemStack stack1 = Util.cleanStack(stack);
            if (provider.hasKnowledge(stack1)) {
                long value = ProjectEAPI.getEMCProxy().getValue(stack1);

                // nothing is paid yet, but only put in what could be paid for alongside the ghost items already there
                if (value > 0L && provider.getEmc().subtract(getReservedEmc()).compareTo(BigInteger.valueOf(value)) >= 0) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, stack1);
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && slotItem.getItem() == stack1.getItem() && Util.areStacksEqual(slotItem, stack1)) {
                        slotItem.grow(1);
                    } else {
                        continue;
                    }

                    ghostCounts[i]++;
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean transferFromInventory(int i, List<ItemStack> possibilities) {
        for (ItemStack possibility : possibilities) {
            ItemStack fixedp = Util.cleanStack(possibility);
            for (int j = 0; j < playerInv.getContainerSize(); ++j) {
                ItemStack stack = playerInv.getItem(j);
                if (canPullFromInventory(stack, Util.cleanStack(stack), possibility, fixedp)) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, ItemHandlerHelper.copyStackWithSize(stack, 1));
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && Util.areStacksEqual(slotItem, stack)) {
                        slotItem.grow(1);
                    } else {
                        continue;
                    }

                    stack.shrink(1);

                    if (stack.isEmpty()) {
                        playerInv.setItem(j, ItemStack.EMPTY);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean canPullFromInventory(ItemStack stack, ItemStack cleanStack, ItemStack possibility, ItemStack cleanPossibility) {
        if (stack.isEmpty() || EmcLock.isLocked(stack)) return false;
        // items with extra data (enchantments, damage, names, etc.) are only pulled if the recipe asks for exactly that
        if (stack.hasTag() && !Util.isSameItemSameTags(possibility, stack)) return false;
        return Util.areStacksEqual(cleanPossibility, cleanStack);
    }

    /**
     * Works out what {@link #onRecipeTransfer} would do for a single craft, without changing anything.
     * Follows the same order: the grid is cleared, then slots are filled from the inventory, then from transmutation.
     */
    public RecipeTransferPlan planRecipeTransfer(List<List<ItemStack>> recipe) {
        BigInteger available = provider.getEmc();
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < playerInv.getContainerSize(); i++) inventory.add(playerInv.getItem(i).copy());
        // clearing the grid either turns items into emc or puts them back in the inventory
        for (int i = 0; i < CRAFTING_COUNT; i++) {
            // ghost items are thrown away by the clear
            ItemStack stack = ItemHandlerHelper.copyStackWithSize(craftSlots.getItem(i), craftSlots.getItem(i).getCount() - ghostCounts[i]);
            if (stack.isEmpty()) continue;
            if (Util.canConvertWithoutLoss(stack)) {
                available = available.add(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
            } else {
                inventory.add(stack.copy());
            }
        }

        // this runs every frame while a recipe viewer is open, so only clean each stack once
        List<ItemStack> cleanInventory = inventory.stream().map(Util::cleanStack).collect(Collectors.toList());

        int max = Math.min(recipe.size(), craftSlots.getContainerSize());
        boolean[] filled = new boolean[max];
        for (int i = 0; i < max; i++) {
            List<ItemStack> possibilities = recipe.get(i);
            if (possibilities == null || possibilities.stream().allMatch(ItemStack::isEmpty)) {
                filled[i] = true;
                continue;
            }
            search:
            for (ItemStack possibility : possibilities) {
                ItemStack cleanPossibility = Util.cleanStack(possibility);
                for (int j = 0; j < inventory.size(); j++) {
                    ItemStack stack = inventory.get(j);
                    if (canPullFromInventory(stack, cleanInventory.get(j), possibility, cleanPossibility)) {
                        stack.shrink(1);
                        filled[i] = true;
                        break search;
                    }
                }
            }
        }

        BigInteger required = BigInteger.ZERO;
        Set<Integer> excludedSlots = new LinkedHashSet<>();
        List<RecipeTransferPlan.Excluded> excluded = new ArrayList<>();
        BigInteger remaining = available;
        for (int i = 0; i < max; i++) {
            if (filled[i]) continue;
            List<ItemStack> learned = recipe.get(i).stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(Util::cleanStack)
                    .filter(stack -> provider.hasKnowledge(stack) && ProjectEAPI.getEMCProxy().getValue(stack) > 0)
                    .sorted(Comparator.comparingLong(ProjectEAPI.getEMCProxy()::getValue))
                    .collect(Collectors.toList());
            if (learned.isEmpty()) {
                excludedSlots.add(i);
                excluded.add(new RecipeTransferPlan.Excluded(recipe.get(i).stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY), false));
                continue;
            }
            BigInteger cheapest = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(learned.get(0)));
            required = required.add(cheapest);
            // same as transferFromTablet, the cheapest option we can still afford is used
            Optional<ItemStack> affordable = Optional.empty();
            for (ItemStack stack : learned) {
                if (remaining.compareTo(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(stack))) >= 0) {
                    affordable = Optional.of(stack);
                    break;
                }
            }
            if (affordable.isPresent()) {
                remaining = remaining.subtract(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(affordable.get())));
            } else {
                excludedSlots.add(i);
                excluded.add(new RecipeTransferPlan.Excluded(learned.get(0), true));
            }
        }

        BigInteger missing = required.subtract(available).max(BigInteger.ZERO);
        return new RecipeTransferPlan(required, missing, excludedSlots, excluded);
    }

    public void transferItems(List<List<ItemStack>> recipe, int max) {
        for (int i = 0; i < max; ++i) {
            if (recipe.get(i) != null && !recipe.get(i).isEmpty()) {
                transferFromInventory(i, recipe.get(i));
            }
        }

        for (int i = 0; i < max; ++i) {
            if (recipe.get(i) != null && !recipe.get(i).isEmpty()) {
                transferFromTablet(i, recipe.get(i));
            }
        }

    }

    /** @apiNote only call on server */
    public void clearCrafting(boolean force) {
        boolean emcUpdate = false;
        discardGhosts();
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) continue;

            if (Util.canConvertWithoutLoss(stack)) {
                Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, stack);

                if (!result.fail()) {
                    BigInteger emc = provider.getEmc().add(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
                    provider.setEmc(emc);
                    emcUpdate = true;
                    craftSlots.setItem(i, ItemStack.EMPTY);
                    continue;
                }
            }

            ItemStack result = Util.returnToInventory(playerInv, player, stack, force);
            craftSlots.setItem(i, result);
        }

        if (emcUpdate && player instanceof ServerPlayerEntity) provider.syncEmc(((ServerPlayerEntity) player));
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void rotateCrafting(boolean clockwise) {
        ItemStack[] stacks = new ItemStack[ROTATION_SLOTS.length];
        int[] ghosts = new int[ROTATION_SLOTS.length];

        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            int from = clockwise ? ROTATION_SLOTS[(i - 1 + ROTATION_SLOTS.length) % ROTATION_SLOTS.length] : ROTATION_SLOTS[(i + 1) % ROTATION_SLOTS.length];
            stacks[i] = craftSlots.getItem(from);
            ghosts[i] = ghostCounts[from];
        }

        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            craftSlots.setItem(ROTATION_SLOTS[i], stacks[i]);
            ghostCounts[ROTATION_SLOTS[i]] = ghosts[i];
        }

        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void balanceCrafting() {
        keepGhosts(this::doBalanceCrafting);
    }

    private void doBalanceCrafting() {
        ArrayListMultimap<CompoundNBT, ItemStack> map = ArrayListMultimap.create();
        Multiset<CompoundNBT> itemCount = HashMultiset.create();

        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                CompoundNBT tag = stack.save(new CompoundNBT());
                tag.remove("Count");
                map.put(tag, stack);
                itemCount.add(tag, stack.getCount());
            }
        }

        for (CompoundNBT tag : map.keySet()) {
            List<ItemStack> list = map.get(tag);
            int totalCount = itemCount.count(tag);
            int countPerStack = totalCount / list.size();
            int restCount = totalCount % list.size();

            for (ItemStack stack : list) stack.setCount(countPerStack);

            int idx = 0;
            while (restCount > 0) {
                ItemStack stack = list.get(idx);

                if (stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    restCount--;
                }

                idx++;

                if (idx >= list.size()) {
                    idx = 0;
                }
            }
        }


        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void spreadCrafting() {
        keepGhosts(this::doSpreadCrafting);
    }

    private void doSpreadCrafting() {
        while (true) {
            ItemStack biggestStack = null;
            int biggestSize = 1;

            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.getItem(i);

                if (!stack.isEmpty() && stack.getCount() > biggestSize) {
                    biggestStack = stack;
                    biggestSize = stack.getCount();
                }
            }

            if (biggestStack == null) {
                return;
            }

            boolean emptyBiggestSlot = false;

            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.getItem(i);

                if (stack.isEmpty()) {
                    if (biggestStack.getCount() > 1) {
                        craftSlots.setItem(i, biggestStack.split(1));
                    } else {
                        emptyBiggestSlot = true;
                    }
                }
            }

            if (!emptyBiggestSlot) {
                break;
            }
        }

        doBalanceCrafting();
    }

    /** @apiNote only call on client */
    public void action(PacketArcaneTransmutationTabletSmallButton.Action action) {
        PacketHandler.sendToServer(new PacketArcaneTransmutationTabletSmallButton(action));
    }

    public IKnowledgeProvider getProvider() {
        return provider;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public ImmutableList<PXOutputSlot> getOutputSlots() {
        return ImmutableList.copyOf(outputSlots);
    }
}
