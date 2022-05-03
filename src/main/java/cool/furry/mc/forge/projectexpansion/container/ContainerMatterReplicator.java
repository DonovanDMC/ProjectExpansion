package cool.furry.mc.forge.projectexpansion.container;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.inventory.ItemhandlerMatterReplicator;
import cool.furry.mc.forge.projectexpansion.container.slots.SlotMatter;
import cool.furry.mc.forge.projectexpansion.container.slots.SlotUpgrade;
import cool.furry.mc.forge.projectexpansion.init.ContainerTypes;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;

public class ContainerMatterReplicator extends Container {
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 75;
    public static final int PLAYER_HOTBAR_X = 8;
    public static final int PLAYER_HOTBAR_Y = 133;
    public static final int UPGRADES_X = 134;
    public static final int UPGRADES_Y = 54;
    public static final int INPUT_X = 80;
    public static final int INPUT_Y = 8;
    public static final int OUTPUT_X = 80;
    public static final int OUTPUT_Y = 54;
    public static final int ARROW_X = 79;
    public static final int ARROW_Y = 27;
    public static final int ARROW_WIDTH = 17;
    public static final int ARROW_HEIGHT = 24;
    public static final int ARROW_FILLED_X = 176;
    public static final int ARROW_FILLED_Y = 0;

    public TileMatterReplicator tile;
    public ContainerMatterReplicator(int id, PlayerInventory playerInventory, PacketBuffer packetBuffer) {
        this(id, playerInventory, packetBuffer.readBlockPos());
    }
    public ContainerMatterReplicator(int id, PlayerInventory playerInventory, BlockPos pos) {
        super(ContainerTypes.MATTER_REPLICATOR.get(), id);
        TileEntity tile = playerInventory.player.getEntityWorld().getTileEntity(pos);
        assert (tile instanceof  TileMatterReplicator);
        this.tile = (TileMatterReplicator) tile;
        Util.addPlayerInventoryToContainer(this::addSlot, playerInventory, PLAYER_HOTBAR_X, PLAYER_HOTBAR_Y, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        addSlot(new SlotUpgrade(this.tile.containerItemHandler, 0, UPGRADES_X, UPGRADES_Y, ItemUpgrade.UpgradeType.SPEED));
        addSlot(new SlotUpgrade(this.tile.containerItemHandler, 1, UPGRADES_X + Util.SLOT_SPACING_X, UPGRADES_Y, ItemUpgrade.UpgradeType.STACK));
        addSlot(new SlotMatter(this.tile.containerItemHandler, 2, INPUT_X, INPUT_Y));
        addSlot(new SlotMatter(this.tile.containerItemHandler, 3, OUTPUT_X, OUTPUT_Y));
    }

    public double percentageToUnlock() {
        if(!tile.isLocked) return 100;
        double div = (double) tile.getLockedTime() / (double) tile.getGenTime();
        return MathHelper.clamp(div, 0.0, 0.0);
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack before = stack.copy();

        SlotZone zone = SlotZone.getZoneFromIndex(index);
        boolean success = false;
        switch(zone) {
            case OUTPUT: {
                success = mergeInto(SlotZone.PLAYER_HOTBAR, stack, true);
                if (!success) success = mergeInto(SlotZone.PLAYER_INVENTORY, stack, true);
                if (success) slot.onSlotChange(stack, before);
                break;
            }

            case INPUT: {
                // ignore
                break;
            }

            case PLAYER_HOTBAR:
            case PLAYER_INVENTORY: {
                if (Util.isMatter(stack)) success = mergeInto(SlotZone.INPUT, stack, false);
                if (!success) success = mergeInto(SlotZone.SPEED_UPGRADE, stack, false);
                if (!success) success = mergeInto(SlotZone.STACK_UPGRADE, stack, false);
                if (!success) {
                    if (zone == SlotZone.PLAYER_HOTBAR) success = mergeInto(SlotZone.PLAYER_INVENTORY, stack, false);
                    else success = mergeInto(SlotZone.PLAYER_HOTBAR, stack, false);
                }
                break;
            }

            case SPEED_UPGRADE: {
                success = mergeInto(SlotZone.PLAYER_HOTBAR, stack, false);
                if(!success) success = mergeInto(SlotZone.PLAYER_INVENTORY, stack, false);
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

    @Override
    public void putStackInSlot(int slotID, ItemStack stack) {
        super.putStackInSlot(slotID, stack);
    }

    /* Borrowed with love from Minecraft By Example
     * https://github.com/TheGreyGhost/MinecraftByExample/blob/master/src/main/java/minecraftbyexample/mbe31_inventory_furnace/ContainerFurnace.java
     */

    private boolean mergeInto(SlotZone destinationZone, ItemStack sourceItemStack, boolean fillFromEnd) {
        return mergeItemStack(sourceItemStack, destinationZone.firstIndex, destinationZone.lastIndexPlus1, fillFromEnd);
    }

    private enum SlotZone {
        OUTPUT(39, 1),
        INPUT(38, 1),
        STACK_UPGRADE(37, 1),
        SPEED_UPGRADE(36, 1),
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
