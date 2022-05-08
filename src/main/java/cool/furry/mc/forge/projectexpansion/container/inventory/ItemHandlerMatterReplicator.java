package cool.furry.mc.forge.projectexpansion.container.inventory;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static cool.furry.mc.forge.projectexpansion.item.ItemUpgrade.UpgradeType;
import static cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator.*;

public class ItemHandlerMatterReplicator implements IItemHandlerModifiable {
    private final IIntArray data;
    private final TileMatterReplicator tile;
    public ItemHandlerMatterReplicator(IIntArray data, TileMatterReplicator tile) {
        this.data = data;
        this.tile = tile;
    }

    private boolean isLocked() {
        return data.get(LOCKED) == 1;
    }

    private void lock() {
        data.set(LOCKED, 1);
    }

    private int getSpeedUpgradeCount() {
        return data.get(SPEED_UPGRADE_COUNT);
    }

    private void setSpeedUpgradeCount(int value) {
        data.set(SPEED_UPGRADE_COUNT, value);
    }

    private int getStackUpgradeCount() {
        return data.get(STACK_UPGRADE_COUNT);
    }

    private void setStackUpgradeCount(int value) {
        data.set(STACK_UPGRADE_COUNT, value);
    }


    private int getStackLimit() {
        return tile.getItem() == Items.FINAL_STAR_SHARD.get() ? 1 : getStackUpgradeCount() == 0 ? 1 : (int) Math.pow(2, getStackUpgradeCount());
    }

    @Override
    public int getSlots() {
        return 4;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        switch (slot) {
            case SPEED_UPGRADE_SLOT: return getSpeedUpgradeCount() == 0 ? ItemStack.EMPTY : new ItemStack(Items.SPEED_UPGRADE.get(), getSpeedUpgradeCount());
            case STACK_UPGRADE_SLOT: return getStackUpgradeCount() == 0 ? ItemStack.EMPTY : new ItemStack(Items.STACK_UPGRADE.get(), getStackUpgradeCount());
            case INPUT_SLOT: return tile.getItemStack();
            case OUTPUT_SLOT: return isLocked() ? ItemStack.EMPTY : new ItemStack(tile.getItem(), getStackLimit());
            default: return ItemStack.EMPTY;
        }
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        stack = stack.copy();
        switch(slot) {
            case SPEED_UPGRADE_SLOT: {
                int insertCount = Math.min(stack.getCount(), UpgradeType.SPEED.getMax() - getSpeedUpgradeCount());
                if(!simulate) setSpeedUpgradeCount(getSpeedUpgradeCount() + insertCount);
                if(insertCount == stack.getCount()) return ItemStack.EMPTY;
                stack.setCount(stack.getCount() - insertCount);
                return stack;
            }
            case STACK_UPGRADE_SLOT: {
                int insertCount = Math.min(stack.getCount(), UpgradeType.STACK.getMax() - getStackUpgradeCount());
                if(!simulate) setStackUpgradeCount(getStackUpgradeCount() + insertCount);
                if(insertCount == stack.getCount()) return ItemStack.EMPTY;
                stack.setCount(stack.getCount() - insertCount);
                return stack;

            }
            case INPUT_SLOT: {
                if(!tile.getItemStack().isEmpty()) return stack;
                tile.setItem(stack.getItem());
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
            case SPEED_UPGRADE_SLOT: {
                if(count > getSpeedUpgradeCount()) count = getSpeedUpgradeCount();
                if(!simulate) setSpeedUpgradeCount(getSpeedUpgradeCount() - count);
                return new ItemStack(Items.SPEED_UPGRADE.get(), count);
            }
            case STACK_UPGRADE_SLOT: {
                if(count > getStackUpgradeCount()) count = getStackUpgradeCount();
                if(!simulate) setStackUpgradeCount(getStackUpgradeCount() - count);
                return new ItemStack(Items.STACK_UPGRADE.get(), count);
            }
            case INPUT_SLOT: {
                // we have to return something for simulate because this doesn't work otherwise
                if(!simulate) tile.setItemStack(ItemStack.EMPTY);
                return simulate ? tile.getItemStack() : ItemStack.EMPTY;
            }
            case OUTPUT_SLOT: {
                if(tile.getItemStack().isEmpty() || isLocked()) return ItemStack.EMPTY;
                if(!simulate) lock();
                return new ItemStack(tile.getItem(), getStackLimit());
            }
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SPEED_UPGRADE_SLOT ? UpgradeType.SPEED.getMax() : slot == STACK_UPGRADE_SLOT ? UpgradeType.STACK.getMax() : slot == INPUT_SLOT ? 1 : getStackLimit();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return
            slot == SPEED_UPGRADE_SLOT ? tile.getItem() == Items.SPEED_UPGRADE.get() :
                slot == STACK_UPGRADE_SLOT ? tile.getItem() == Items.STACK_UPGRADE.get() :
                    Arrays.stream(Matter.VALUES).anyMatch((m) -> m.getItem() == stack.getItem());
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        insertItem(slot, stack, false);
    }
}
