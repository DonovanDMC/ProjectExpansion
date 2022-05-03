package cool.furry.mc.forge.projectexpansion.container.inventory;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryMatterReplicator implements IInventory {
    private final TileMatterReplicator tile;
    public InventoryMatterReplicator(TileMatterReplicator tile) {
        this.tile = tile;
    }
    @Override
    public int getSizeInventory() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        // input slot does not count towards active slots
        return tile.speedUpgradeCount == 0 && tile.stackUpgradeCount == 0 && (tile.itemStack.isEmpty() || tile.verifyLock());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        switch(slot) {
            case TileMatterReplicator.SPEED_UPGRADE_SLOT: return tile.speedUpgradeCount == 0 ? ItemStack.EMPTY : new ItemStack(Items.SPEED_UPGRADE.get(), tile.speedUpgradeCount);
            case TileMatterReplicator.STACK_UPGRADE_SLOT: return tile.stackUpgradeCount == 0 ? ItemStack.EMPTY : new ItemStack(Items.STACK_UPGRADE.get(), tile.stackUpgradeCount);
            case TileMatterReplicator.INPUT_SLOT: return tile.itemStack;
            case TileMatterReplicator.OUTPUT_SLOT: return tile.verifyLock() ? ItemStack.EMPTY : new ItemStack(tile.itemStack.getItem(), tile.getStackLimit());
            default: return ItemStack.EMPTY;
        }
    }

    private ItemStack extractItem(int index, int count) {
        switch(index) {
            case TileMatterReplicator.SPEED_UPGRADE_SLOT: {
                if(count > tile.speedUpgradeCount) count = tile.speedUpgradeCount;
                tile.speedUpgradeCount = tile.speedUpgradeCount - count;
                markDirty();
                return new ItemStack(Items.SPEED_UPGRADE.get(), count);
            }
            case TileMatterReplicator.STACK_UPGRADE_SLOT: {
                if(count > tile.stackUpgradeCount) count = tile.stackUpgradeCount;
                tile.stackUpgradeCount = tile.stackUpgradeCount - count;
                markDirty();
                return new ItemStack(Items.STACK_UPGRADE.get(), count);
            }
            case TileMatterReplicator.INPUT_SLOT: {
                tile.itemStack = ItemStack.EMPTY;
                markDirty();
                return ItemStack.EMPTY;
            }
            case TileMatterReplicator.OUTPUT_SLOT: {
                if(tile.itemStack.isEmpty() || tile.verifyLock()) return ItemStack.EMPTY;
                tile.lock();
                return new ItemStack(tile.itemStack.getItem(), tile.getStackLimit());
            }
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return extractItem(index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return extractItem(index, getStackInSlot(index).getCount());
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        switch(index) {
            case TileMatterReplicator.SPEED_UPGRADE_SLOT: tile.speedUpgradeCount = stack.getCount(); break;
            case TileMatterReplicator.STACK_UPGRADE_SLOT: tile.stackUpgradeCount = stack.getCount(); break;
            case TileMatterReplicator.INPUT_SLOT: {
                ItemStack copy = stack.copy();
                if(copy.getCount() != 1) copy.setCount(1);
                tile.itemStack = copy;
                break;
            }
            case TileMatterReplicator.OUTPUT_SLOT: {
                // ignore
            }
        }
    }

    @Override
    public void markDirty() {
        tile.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        tile.speedUpgradeCount = 0;
        tile.stackUpgradeCount = 0;
        tile.itemStack = ItemStack.EMPTY;
        markDirty();
    }
}
