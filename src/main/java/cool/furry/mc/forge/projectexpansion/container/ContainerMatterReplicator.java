package cool.furry.mc.forge.projectexpansion.container;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.inventory.ItemHandlerMatterReplicator;
import cool.furry.mc.forge.projectexpansion.container.slots.SlotMatter;
import cool.furry.mc.forge.projectexpansion.container.slots.SlotUpgrade;
import cool.furry.mc.forge.projectexpansion.init.ContainerTypes;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.logging.log4j.Level;

import static cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator.TIME_BASE;
import static cool.furry.mc.forge.projectexpansion.item.ItemUpgrade.UpgradeType;

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

    private final IIntArray data;
    private TileMatterReplicator tile;
    @SuppressWarnings("unused")
    public ContainerMatterReplicator(int id, PlayerInventory playerInventory, PacketBuffer packetBuffer) {
        this(id, playerInventory, packetBuffer.readBlockPos(), new IntArray(4));
    }
    public ContainerMatterReplicator(int id, PlayerInventory playerInventory, BlockPos pos, IIntArray data) {
        super(ContainerTypes.MATTER_REPLICATOR.get(), id);
        this.data = data;
        TileEntity tile = playerInventory.player.getEntityWorld().getTileEntity(pos);
        if (!(tile instanceof TileMatterReplicator)) return;
        this.tile = (TileMatterReplicator) tile;
        ItemHandlerMatterReplicator handler = new ItemHandlerMatterReplicator(data, this.tile);
        Util.addPlayerInventoryToContainer(this::addSlot, playerInventory, PLAYER_HOTBAR_X, PLAYER_HOTBAR_Y, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        addSlot(new SlotUpgrade(handler, 0, UPGRADES_X, UPGRADES_Y, UpgradeType.SPEED));
        addSlot(new SlotUpgrade(handler, 1, UPGRADES_X + Util.SLOT_SPACING_X, UPGRADES_Y, UpgradeType.STACK));
        addSlot(new SlotMatter(handler, 2, INPUT_X, INPUT_Y));
        addSlot(new SlotMatter(handler, 3, OUTPUT_X, OUTPUT_Y));
        assertIntArraySize(data, 4);
        trackIntArray(data);
    }

    public boolean isLocked() {
        return data.get(TileMatterReplicator.LOCKED) == 1;
    }

    private int getLockedTicks() {
        return data.get(TileMatterReplicator.LOCKED_TICKS);
    }

    private int getSpeedUpgradeCount() {
        return data.get(TileMatterReplicator.SPEED_UPGRADE_COUNT);
    }

    private int getGenTime() {
        int time = TIME_BASE - (getSpeedUpgradeCount() * 10);
        return Math.max(time, 1);
    }

    public double percentageToUnlock() {
        if(!isLocked()) return 1.0;
        double div = (double) getLockedTicks() / (double) getGenTime();
        return MathHelper.clamp(div, 0.0, 1.0);
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
                if (Util.isMatter(stack) && tile.getItemStack() == ItemStack.EMPTY) {
                    tile.setItemStack(stack);
                    success = true;
                }
                if (!success) success = mergeInto(SlotZone.SPEED_UPGRADE, stack, false);
                if (!success) success = mergeInto(SlotZone.STACK_UPGRADE, stack, false);
                if (!success) {
                    if (zone == SlotZone.PLAYER_HOTBAR) success = mergeInto(SlotZone.PLAYER_INVENTORY, stack, false);
                    else success = mergeInto(SlotZone.PLAYER_HOTBAR, stack, false);
                }
                break;
            }

            case SPEED_UPGRADE:
            case STACK_UPGRADE: {
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
