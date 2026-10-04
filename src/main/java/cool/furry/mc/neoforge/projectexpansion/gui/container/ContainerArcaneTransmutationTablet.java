package cool.furry.mc.neoforge.projectexpansion.gui.container;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import cool.furry.mc.neoforge.projectexpansion.gui.container.slots.PXCraftingSlot;
import cool.furry.mc.neoforge.projectexpansion.gui.container.slots.PXInputSlot;
import cool.furry.mc.neoforge.projectexpansion.gui.container.slots.PXOutputSlot;
import cool.furry.mc.neoforge.projectexpansion.gui.container.slots.PXResultSlot;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.neoforge.projectexpansion.util.ContainerData;
import cool.furry.mc.neoforge.projectexpansion.util.EmcLock;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.container.PEHandContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.*;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

public class ContainerArcaneTransmutationTablet extends PEHandContainer {
    private static final int[] ROTATION_SLOTS = {0, 1, 2, 5, 8, 7, 6, 3};
    private final List<SlotInput> inputSlots = new ArrayList<>();
    private final List<PXOutputSlot> outputSlots = new ArrayList<>();
    public final TransmutationInventory transmutationInventory;
    private TransientCraftingContainer craftSlots;
    private ResultContainer resultSlots;
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
    private final Player player;
    private final IKnowledgeProvider provider;
    public boolean isCrafting = false;
    public boolean skipRefill = false;

    public static ContainerArcaneTransmutationTablet fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        @Nullable IKnowledgeProvider provider = playerInv.player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (provider == null) throw new NullPointerException("provider is null");
        ContainerData data = ContainerData.decode(buf);
        return new ContainerArcaneTransmutationTablet(windowId, playerInv, provider, data.hand().orElse(null), data.selected());
    }

    public ContainerArcaneTransmutationTablet(int windowId, Inventory playerInv, IKnowledgeProvider provider) {
        this(windowId, playerInv, provider, null, 0);
    }

    public ContainerArcaneTransmutationTablet(int windowId, Inventory playerInv, IKnowledgeProvider provider, @Nullable InteractionHand hand, int selected) {
        super(new ContainerTypeRegistryObject<>(MenuTypes.ARCANE_TRANSMUTATION_TABLET.getKey()), windowId, playerInv, hand, selected);
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
        addPlayerInventory(8, 135);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        addSlot(new PXResultSlot(playerInv.player, this.craftSlots, this.resultSlots, this,0, -23, 75));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new PXCraftingSlot(this.craftSlots, j + i * 3, -59 + j * 18, 17 + i * 18));
            }
        }

    }

    @Override
    protected Slot addSlot(Slot slot) {
        switch (slot) {
            case PXInputSlot inputSlot -> this.inputSlots.add(inputSlot);
            case PXOutputSlot outputSlot -> this.outputSlots.add(outputSlot);
            default -> {}
        }
        return super.addSlot(slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            player.drop(unlearn.getItem(), false);
            for (ItemStack stack : this.craftSlots.getItems()) {
                player.drop(stack, false);
            }
            for (int i = 0; i < this.resultSlots.getContainerSize(); i++) {
                player.drop(this.resultSlots.getItem(i), false);
            }
        } else {
            Util.returnToInventoryOrTransmutation(playerInv, player, provider, unlearn.getItem(), false, true);
            for (ItemStack stack : this.craftSlots.getItems()) {
                Util.returnToInventoryOrTransmutation(playerInv, player, provider, stack, false, true);
            }
            for (int i = 0; i < this.resultSlots.getContainerSize(); i++) {
                Util.returnToInventoryOrTransmutation(playerInv, player, provider, this.resultSlots.getItem(i), false, true);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (this.tryGetSlot(slotIndex) instanceof PXCraftingSlot slot && slot.hasItem()) {
            ItemStack itemStack = slot.getItem().copy();
            this.moveItemStackTo(itemStack, PLAYER, (PLAYER + PLAYER_COUNT - 1), true);
            slot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        if (this.tryGetSlot(slotIndex) instanceof PXResultSlot || slotIndex == RESULT) {
            Slot slot = this.tryGetSlot(slotIndex);
            if (slot != null && slot.hasItem()) {
                ItemStack itemStack1 = slot.getItem();
                ItemStack itemStack = itemStack1.copy();
                itemStack1.getItem().onCraftedBy(itemStack, player.level(), player);
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
            long itemEmc = IEMCProxy.INSTANCE.getValue(stack);
            //Double-check the item actually has Emc and something didn't just go terribly wrong
            if (itemEmc > 0) {
                //Note: We can just set the size here as newStack is a copy stack used for modifications
                stack.setCount(stack.getMaxStackSize());
                //Check how much we can fit of the stack
                int itemsRoomFor = stack.getCount() - ItemHelper.simulateFit(player.getInventory().items, stack);
                if (itemsRoomFor == 1) {
                    long availableEMC = transmutationInventory.getAvailableEmcAsLong();
                    if (itemEmc > availableEMC) {
                        //We need more EMC than we have available, but we were only trying to get a single item.
                        // This means we can't actually produce any of the item
                        return ItemStack.EMPTY;
                    }
                    if (transmutationInventory.isServer()) {
                        transmutationInventory.removeEmc(BigInteger.valueOf(itemEmc));
                    }
                    stack.setCount(1);
                    ItemHandlerHelper.insertItemStacked(player.getCapability(Capabilities.ItemHandler.ENTITY), stack, false);
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
                    ItemHandlerHelper.insertItemStacked(player.getCapability(Capabilities.ItemHandler.ENTITY), stack, false);
                }
            }
        } else if (slotIndex >= (OUTPUT + OUTPUT_COUNT) && slotIndex < CRAFTING) {
            ItemStack slotStack = currentSlot.getItem();
            if (EmcLock.isLocked(slotStack)) return ItemStack.EMPTY;
            ItemStack stackToInsert = slotStack;
            if (stackToInsert.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
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
            long emc = IEMCProxy.INSTANCE.getSellValue(stackToInsert);
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
    public void clickPostValidate(int slotIndex, int dragType, ClickType clickType, Player player) {
        if (player.level().isClientSide && transmutationInventory.getHandlerForSlot(slotIndex) == transmutationInventory.outputs && tryGetSlot(slotIndex) instanceof Slot slot) {
            PacketDistributor.sendToServer(new SearchUpdatePKT(transmutationInventory.getIndexFromSlot(slotIndex), slot.getItem()));
        }
        super.clickPostValidate(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof SlotConsume || slot instanceof SlotUnlearn || slot instanceof SlotInput || slot instanceof SlotLock || slot instanceof SlotOutput);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.QUICK_MOVE) skipRefill = true;
        super.clicked(slotId, dragType, clickType, player);;
        if (clickType == ClickType.QUICK_MOVE) skipRefill = false;
    }

    @Override
    public void slotsChanged(Container container) {
        slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots);
        super.slotsChanged(container);
    }

    protected static void slotChangedCraftingGrid(ContainerArcaneTransmutationTablet menu, Level level, Player player, CraftingContainer crafting, ResultContainer result) {
        if (!level.isClientSide) {
            CraftingInput input = CraftingInput.of(3, 3, crafting.getItems());
            RegistryAccess registryAccess = level.registryAccess();
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ItemStack stack = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> optional = (Objects.requireNonNull(level.getServer())).getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (optional.isPresent()) {
                RecipeHolder<CraftingRecipe> recipe = optional.get();
                if (result.setRecipeUsed(level, serverPlayer, recipe)) {
                    stack = recipe.value().assemble(input, registryAccess);
                }
            }

            result.setItem(0, stack);
            menu.setRemoteSlot(0, stack);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, stack));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public void onRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll) {
        clearCrafting(false);
        fillCraftingSlots(recipe, transferAll);
    }

    public void fillCraftingSlots(List<List<ItemStack>> recipe, boolean transferAll) {
        int max = Math.min(recipe.size(), craftSlots.getContainerSize());
        transferItems(recipe, max);

        if (transferAll) {
            for (int i = 0; i < 63; i++) {
                transferItems(recipe, max);
            }
        }

        if (player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean transferFromTablet(int i, List<ItemStack> possibilities) {
        if (!possibilities.isEmpty()) {
            possibilities.sort(Comparator.comparingLong(IEMCProxy.INSTANCE::getValue));
        }

        for (ItemStack stack : possibilities) {
            ItemStack stack1 = Util.cleanStack(stack);
            if (provider.hasKnowledge(stack1)) {
                long value = IEMCProxy.INSTANCE.getValue(stack1);

                if (value > 0L && provider.getEmc().compareTo(BigInteger.valueOf(value)) >= 0) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, stack1);
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && slotItem.getItem() == stack1.getItem() && Util.areStacksEqual(player.registryAccess(), slotItem, stack1)) {
                        slotItem.grow(1);
                    } else {
                        continue;
                    }

                    provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(value)));
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
                if (Util.areStacksEqual(player.registryAccess(), fixedp, Util.cleanStack(stack))) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, stack.copyWithCount(1));
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && Util.areStacksEqual(player.registryAccess(), slotItem, stack)) {
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
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) continue;

            if (ProjectEConfig.server.difficulty.covalenceLoss.get() >= 1D && IEMCProxy.INSTANCE.hasValue(stack)) {
                Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, stack);

                if (!result.fail()) {
                    BigInteger emc = provider.getEmc().add(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
                    provider.setEmc(emc);
                    emcUpdate = true;
                    craftSlots.setItem(i, ItemStack.EMPTY);
                    continue;
                }
            }

            ItemStack result = Util.returnToInventory(playerInv, player, stack, force);
            craftSlots.setItem(i, result);
        }

        if (emcUpdate && player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void rotateCrafting(boolean clockwise) {
        ItemStack[] stacks = new ItemStack[ROTATION_SLOTS.length];

        if (clockwise) {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                int j = i - 1;

                if (j < 0) j = ROTATION_SLOTS.length - 1;
                stacks[i] = craftSlots.getItem(ROTATION_SLOTS[j % ROTATION_SLOTS.length]);
            }
        } else {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                stacks[i] = craftSlots.getItem(ROTATION_SLOTS[(i + 1) % ROTATION_SLOTS.length]);
            }
        }

        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            craftSlots.setItem(ROTATION_SLOTS[i], stacks[i]);
        }

        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void balanceCrafting() {
        ArrayListMultimap<CompoundTag, ItemStack> map = ArrayListMultimap.create();
        Multiset<CompoundTag> itemCount = HashMultiset.create();

        RegistryAccess registryAccess = player.registryAccess();
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                CompoundTag tag = (CompoundTag) stack.save(registryAccess, new CompoundTag());
                tag.remove("count");
                map.put(tag, stack);
                itemCount.add(tag, stack.getCount());
            }
        }

        for (CompoundTag tag : map.keySet()) {
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

        balanceCrafting();
    }

    /** @apiNote only call on client */
    public void action(PacketArcaneTransmutationTabletSmallButton.Action action) {
        PacketDistributor.sendToServer(new PacketArcaneTransmutationTabletSmallButton(action));
    }

    public IKnowledgeProvider getProvider() {
        return provider;
    }

    public Player getPlayer() {
        return player;
    }

    public ImmutableList<PXOutputSlot> getOutputSlots() {
        return ImmutableList.copyOf(outputSlots);
    }
}
