package cool.furry.mc.forge.projectexpansion.container;

import cool.furry.mc.forge.projectexpansion.container.slots.SlotEMC;
import cool.furry.mc.forge.projectexpansion.init.ContainerTypes;
import cool.furry.mc.forge.projectexpansion.tile.TileEMCImport;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

public class ContainerEMCImport extends Container {
    public static final int INPUT_X = 8;
    public static final int INPUT_Y = 8;

    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 123;
    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = 181;
    private TileEMCImport tile;

    public ContainerEMCImport(final int windowId, final PlayerInventory playerInventory, PacketBuffer extra) {
        super(ContainerTypes.EMC_IMPORT.get(), windowId);
        PlayerInvWrapper invWrapper = new PlayerInvWrapper(playerInventory);

        BlockPos tilePos = extra.readBlockPos();
        TileEntity tile = playerInventory.player.getEntityWorld().getTileEntity(tilePos);
        if (!(tile instanceof TileEMCImport)) return;
        this.tile = (TileEMCImport) tile;

        for (int slot = 0; slot < 9; slot++)
            addSlot(new SlotItemHandler(invWrapper, slot, HOTBAR_X + 18 * slot, HOTBAR_Y));
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlot(new SlotItemHandler(invWrapper, 9 + y * 9 + x, INVENTORY_X + x * 18, INVENTORY_Y + y * 18));

        for (int y = 0; y < 5; y++)
            for (int x = 0; x < 9; x++)
                addSlot(new SlotEMC((IInventory) tile, y * 9 + x, INPUT_X + x * 18, INPUT_Y + y * 18));
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack before = stack.copy();

        SlotZone zone = SlotZone.getZoneFromIndex(index);
        boolean success = false;
        switch (zone) {
            case INPUT: {
                // ignore, disallow shift moving items out
                break;
            }

            case PLAYER_HOTBAR:
            case PLAYER_INVENTORY: {
                if (ProjectEAPI.getEMCProxy().hasValue(stack.getItem()))
                    success = mergeInto(SlotZone.INPUT, stack, false);
                if (!success) {
                    if (zone == SlotZone.PLAYER_HOTBAR) success = mergeInto(SlotZone.PLAYER_INVENTORY, stack, false);
                    else success = mergeInto(SlotZone.PLAYER_HOTBAR, stack, false);
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Invalid sourceZone:" + zone);
        }

        if (!success) return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();

        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;

        return before;
    }

    /* Borrowed with love from Minecraft By Example
     * https://github.com/TheGreyGhost/MinecraftByExample/blob/master/src/main/java/minecraftbyexample/mbe31_inventory_furnace/ContainerFurnace.java
     */

    private boolean mergeInto(SlotZone destinationZone, ItemStack sourceItemStack, boolean fillFromEnd) {
        return mergeItemStack(sourceItemStack, destinationZone.firstIndex, destinationZone.lastIndexPlus1, fillFromEnd);
    }

    private enum SlotZone {
        INPUT(36, 45),
        PLAYER_INVENTORY(9, 27),
        PLAYER_HOTBAR(0, 9);

        public final int firstIndex;
        public final int slotCount;
        public final int lastIndexPlus1;

        SlotZone(int firstIndex, int numberOfSlots) {
            this.firstIndex = firstIndex;
            this.slotCount = numberOfSlots;
            this.lastIndexPlus1 = firstIndex + numberOfSlots;
        }

        public static SlotZone getZoneFromIndex(int slotIndex) {
            for (SlotZone slotZone : SlotZone.values()) {
                if (slotIndex >= slotZone.firstIndex && slotIndex < slotZone.lastIndexPlus1) return slotZone;
            }
            throw new IndexOutOfBoundsException("Unexpected slotIndex");
        }
    }
}
