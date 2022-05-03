package cool.furry.mc.forge.projectexpansion.container.inventory;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemUpgrade;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class ItemhandlerMatterReplicator implements IItemHandlerModifiable {
    private final TileMatterReplicator tile;
    public ItemhandlerMatterReplicator(TileMatterReplicator tile) {
        this.tile = tile;
    }

    @Override
    public int getSlots() {
        return 4;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return tile.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        stack = stack.copy();
        switch(slot) {
            case TileMatterReplicator.SPEED_UPGRADE_SLOT: {
                int insertCount = Math.min(stack.getCount(), ItemUpgrade.UpgradeType.SPEED.getMax() - tile.speedUpgradeCount);
                if(!simulate) tile.speedUpgradeCount += insertCount;
                if(insertCount == stack.getCount()) return ItemStack.EMPTY;
                stack.setCount(stack.getCount() - insertCount);
                return stack;
            }
            case TileMatterReplicator.STACK_UPGRADE_SLOT: {
                int insertCount = Math.min(stack.getCount(), ItemUpgrade.UpgradeType.STACK.getMax() - tile.stackUpgradeCount);
                if(!simulate) tile.stackUpgradeCount += insertCount;
                if(insertCount == stack.getCount()) return ItemStack.EMPTY;
                stack.setCount(stack.getCount() - insertCount);
                return stack;

            }
            case TileMatterReplicator.INPUT_SLOT: {
                if(!tile.itemStack.isEmpty()) return stack;
                tile.itemStack = new ItemStack(stack.getItem(), 1);
                return stack;
            }
            // OUTPUT_SLOT is intentionally not handled
            default: return stack;
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int count = amount;
        switch(slot) {
            case TileMatterReplicator.SPEED_UPGRADE_SLOT: {
                if(count > tile.speedUpgradeCount) count = tile.speedUpgradeCount;
                if(!simulate) {
                    tile.speedUpgradeCount = tile.speedUpgradeCount - count;
                    tile.markDirty();
                }
                return new ItemStack(Items.SPEED_UPGRADE.get(), count);
            }
            case TileMatterReplicator.STACK_UPGRADE_SLOT: {
                if(count > tile.stackUpgradeCount) count = tile.stackUpgradeCount;
                if(!simulate) {
                    tile.stackUpgradeCount = tile.stackUpgradeCount - count;
                    tile.markDirty();
                }
                return new ItemStack(Items.STACK_UPGRADE.get(), count);
            }
            case TileMatterReplicator.INPUT_SLOT: {
                // we have to return something for simulate because this doesn't work otherwise
                if(!simulate) {
                    tile.itemStack = ItemStack.EMPTY;
                    tile.markDirty();
                }
                return simulate ? tile.itemStack : ItemStack.EMPTY;
            }
            case TileMatterReplicator.OUTPUT_SLOT: {
                if(tile.itemStack.isEmpty() || tile.verifyLock()) return ItemStack.EMPTY;
                if(!simulate) tile.lock();
                return new ItemStack(tile.itemStack.getItem(), tile.getStackLimit());
            }
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return tile.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return tile.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        insertItem(slot, stack, false);
    }
}
