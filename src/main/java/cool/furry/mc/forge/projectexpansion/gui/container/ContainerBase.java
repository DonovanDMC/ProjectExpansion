package cool.furry.mc.neoforge.projectexpansion.gui.container;

import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketUpdateWindowBigInteger;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketUpdateWindowInt;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketUpdateWindowLong;
import moze_intel.projecte.gameObjs.container.PEContainer.BoxedLong;
import moze_intel.projecte.gameObjs.container.slots.HotBarSlot;
import moze_intel.projecte.gameObjs.container.slots.IInsertableSlot;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerSlot;
import moze_intel.projecte.gameObjs.container.slots.MainInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// heavily inspired by (stolen from) ProjectE
//https://github.com/sinkillerj/ProjectE/blob/68fbb2dea0cf8a6394fa6c7c084063046d94cee5/src/main/java/moze_intel/projecte/gameObjs/container/PEContainer.java
public abstract class ContainerBase extends AbstractContainerMenu {
    protected final List<InventoryContainerSlot> inventoryContainerSlots = new ArrayList<>();
    protected final List<MainInventorySlot> mainInventorySlots = new ArrayList<>();
    protected final List<HotBarSlot> hotBarSlots = new ArrayList<>();
    // Vanilla only syncs int fields in the superclass as shorts (yay legacy)
    // here we hold fields we really want to use 32 bits for
    private final List<DataSlot> intFields = new ArrayList<>();
    protected final List<BoxedLong> longFields = new ArrayList<>();
    protected final List<BoxedBigInteger> bigIntegerFields = new ArrayList<>();
    protected final Inventory playerInv;

    public ContainerBase(MenuType<?> type, int windowId, Inventory playerInv) {
        super(type, windowId);
        this.playerInv = playerInv;
    }

    protected void addPlayerInventory(int xStart, int yStart) {
        int slotSize = 18;
        int rows = 3;
        //Main Inventory
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(createMainInventorySlot(playerInv, j + i * 9 + 9, xStart + j * slotSize, yStart + i * slotSize));
            }
        }
        yStart = yStart + slotSize * rows + 4;
        //Hot Bar
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            addSlot(createHotBarSlot(playerInv, i, xStart + i * slotSize, yStart));
        }
    }

    protected MainInventorySlot createMainInventorySlot(Inventory inv, int index, int x, int y) {
        return new MainInventorySlot(inv, index, x, y);
    }

    protected HotBarSlot createHotBarSlot(Inventory inv, int index, int x, int y) {
        return new HotBarSlot(inv, index, x, y);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        super.addSlot(slot);
        if (slot instanceof InventoryContainerSlot containerSlot) {
            inventoryContainerSlots.add(containerSlot);
        } else if (slot instanceof MainInventorySlot inventorySlot) {
            mainInventorySlots.add(inventorySlot);
        } else if (slot instanceof HotBarSlot hotBarSlot) {
            hotBarSlots.add(hotBarSlot);
        }
        return slot;
    }

    public @Nullable Slot tryGetSlot(int slotId) {
        if (slotId >= 0 && slotId < slots.size()) {
            return getSlot(slotId);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return The contents in this slot AFTER transferring items away.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotID) {
        Slot currentSlot = tryGetSlot(slotID);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = currentSlot.getItem();
        ItemStack stackToInsert = slotStack;
        if (currentSlot instanceof InventoryContainerSlot) {
            //Insert into stacks that already contain an item in the order hot bar -> main inventory
            stackToInsert = insertItem(hotBarSlots, stackToInsert, true);
            stackToInsert = insertItem(mainInventorySlots, stackToInsert, true);
            //If we still have any left then input into the empty stacks in the order of main inventory -> hot bar
            // Note: Even though we are doing the main inventory, we still need to do both, ignoring empty then not instead of
            // just directly inserting into the main inventory, in case there are empty slots before the one we can stack with
            stackToInsert = insertItem(hotBarSlots, stackToInsert, false);
            stackToInsert = insertItem(mainInventorySlots, stackToInsert, false);
        } else {
            //We are in the main inventory or the hot bar
            //Start by trying to insert it into the block entity's inventory slots, first attempting to stack with other items
            stackToInsert = insertItem(inventoryContainerSlots, stackToInsert, true);
            if (slotStack.getCount() == stackToInsert.getCount()) {
                //Then as long as if we still have the same number of items (failed to insert), try to insert it into the block entity's inventory slots allowing
                // for empty items
                stackToInsert = insertItem(inventoryContainerSlots, stackToInsert, false);
                if (slotStack.getCount() == stackToInsert.getCount()) {
                    //Else if we failed to do that also, try transferring to main inventory or the hot bar, depending on which one we currently are in
                    if (currentSlot instanceof MainInventorySlot) {
                        stackToInsert = insertItem(hotBarSlots, stackToInsert, true);
                        stackToInsert = insertItem(hotBarSlots, stackToInsert, false);
                    } else if (currentSlot instanceof HotBarSlot) {
                        stackToInsert = insertItem(mainInventorySlots, stackToInsert, true);
                        stackToInsert = insertItem(mainInventorySlots, stackToInsert, false);
                    }
                }
            }
        }
        if (stackToInsert.getCount() == slotStack.getCount()) {
            //If nothing changed then return that fact
            return ItemStack.EMPTY;
        }
        //Otherwise, decrease the stack by the amount we inserted, and return it as a new stack for what is now in the slot
        return transferSuccess(currentSlot, player, slotStack, stackToInsert);
    }

    protected ItemStack transferSuccess(Slot currentSlot, Player player, ItemStack slotStack, ItemStack stackToInsert) {
        int difference = slotStack.getCount() - stackToInsert.getCount();
        ItemStack newStack = currentSlot.remove(difference);
        currentSlot.onTake(player, newStack);
        return newStack;
    }

    /**
     * @param slots       Slots to insert into
     * @param stack       Stack to insert (do not modify).
     * @param ignoreEmpty {@code true} to ignore/skip empty slots, {@code false} to ignore/skip non-empty slots.
     *
     * @return Remainder
     */
    public static <SLOT extends Slot & IInsertableSlot> ItemStack insertItem(List<SLOT> slots, ItemStack stack, boolean ignoreEmpty) {
        if (stack.isEmpty()) {
            //Skip doing anything if the stack is already empty.
            // Makes it easier to chain calls, rather than having to check if the stack is empty after our previous call
            return stack;
        }
        for (SLOT slot : slots) {
            if (ignoreEmpty != slot.hasItem()) {
                //Skip checking empty stacks if we want to ignore them, and skipp non-empty stacks if we don't want ot ignore them
                continue;
            }
            stack = slot.insertItem(stack, false);
            if (stack.isEmpty()) {
                break;
            }
        }
        return stack;
    }

    protected static boolean stillValid(Player player, BlockEntity blockEntity, Supplier<Block> block) {
        // return Container.stillValidBlockEntity(blockEntity, player);
        BlockPos pos = blockEntity.getBlockPos();
        return player.level().getBlockState(pos).getBlock() == block.get() &&
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    public final void updateProgressBarLong(int idx, long data) {
        longFields.get(idx).set(data);
    }

    public final void updateProgressBarBigInteger(int idx, BigInteger data) {
        bigIntegerFields.get(idx).set(data);
    }

    public final void updateProgressBarInt(int idx, int data) {
        intFields.get(idx).set(data);
    }

    @Override
    protected DataSlot addDataSlot(DataSlot referenceHolder) {
        intFields.add(referenceHolder);
        return referenceHolder;
    }

    protected void broadcastPX(boolean all) {
        //Note: We use the old way of comparing if it is dirty rather than storing a separate list
        // and comparing entries as there is no real reason to do that if we already have a concept
        // of if it is dirty or not
        for (int i = 0; i < longFields.size(); i++) {
            BoxedLong boxedLong = longFields.get(i);
            //Note: Check all after isDirty as the isDirty resets the dirty state
            if (boxedLong.isDirty() || all) {
                syncDataChange(new PacketUpdateWindowLong((short) containerId, (short) i, boxedLong.get()));
            }
        }

        for (int i = 0; i < intFields.size(); i++) {
            DataSlot referenceHolder = intFields.get(i);
            //Note: Check all after isDirty as the isDirty resets the dirty state
            if (referenceHolder.checkAndClearUpdateFlag() || all) {
                syncDataChange(new PacketUpdateWindowInt((short) containerId, (short) i, referenceHolder.get()));
            }
        }

        for (int i = 0; i < bigIntegerFields.size(); i++) {
            BoxedBigInteger boxedBigInteger = bigIntegerFields.get(i);
            //Note: Check all after isDirty as the isDirty resets the dirty state
            if (boxedBigInteger.isDirty() || all) {
                syncDataChange(new PacketUpdateWindowBigInteger((short) containerId, (short) i, boxedBigInteger.get()));
            }
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        broadcastPX(false);
    }

    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        broadcastPX(true);
    }

    protected void syncDataChange(IPacket packet) {
        //Note: We ignore suppressRemoteUpdates as that is mostly used as a hack for slot syncing
        // (which we don't sync with this) and also we would have to AT in to access it
        if (this.playerInv.player instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    public static class BoxedBigInteger {
        // ProjectE's BoxedLong uses the primitive long type, which is not nullable, so for our implementation to behave
        // similarly, we need to set a default
        private BigInteger inner = BigInteger.ZERO;
        private boolean dirty = false;

        public BoxedBigInteger() {
        }

        public BigInteger get() {
            return this.inner;
        }

        public void set(BigInteger v) {
            if (!v.equals(this.inner)) {
                this.inner = v;
                this.dirty = true;
            }

        }

        public boolean isDirty() {
            boolean ret = this.dirty;
            this.dirty = false;
            return ret;
        }
    }
}
