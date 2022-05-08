package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.container.ContainerMatterReplicator;
import cool.furry.mc.forge.projectexpansion.container.inventory.ItemHandlerMatterReplicator;
import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import static cool.furry.mc.forge.projectexpansion.item.ItemUpgrade.UpgradeType;

public class TileMatterReplicator extends TileEntity implements ITickableTileEntity, IItemHandler, INamedContainerProvider {
    public static final int SPEED_UPGRADE_SLOT = 0;
    public static final int STACK_UPGRADE_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int TIME_BASE = 200;
    public int speedUpgradeCount = 0;
    public int stackUpgradeCount = 0;
    public ItemStack itemStack;
    public int lockedTicks = 0;
    public boolean isLocked = false;
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    public final IIntArray data = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case LOCKED: return TileMatterReplicator.this.isLocked ? 1 : 0;
                case LOCKED_TICKS: return TileMatterReplicator.this.lockedTicks;
                case SPEED_UPGRADE_COUNT: return TileMatterReplicator.this.speedUpgradeCount;
                case STACK_UPGRADE_COUNT: return TileMatterReplicator.this.stackUpgradeCount;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch(index) {
                case LOCKED: TileMatterReplicator.this.isLocked = value == 1; break;
                case LOCKED_TICKS: TileMatterReplicator.this.lockedTicks = value; break;
                case SPEED_UPGRADE_COUNT: TileMatterReplicator.this.speedUpgradeCount = value; break;
                case STACK_UPGRADE_COUNT: TileMatterReplicator.this.stackUpgradeCount = value; break;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };
    public static final int LOCKED = 0;
    public static final int LOCKED_TICKS = 1;
    public static final int SPEED_UPGRADE_COUNT = 2;
    public static final int STACK_UPGRADE_COUNT = 3;

    public TileMatterReplicator() {
        super(TileEntityTypes.MATTER_REPLICATOR.get());
        itemStack = ItemStack.EMPTY;
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.contains("SpeedUpgradeCount", Constants.NBT.TAG_INT))
            speedUpgradeCount = nbt.getInt("SpeedUpgradeCount");
        if (nbt.contains("StackUpgradeCount", Constants.NBT.TAG_INT))
            stackUpgradeCount = nbt.getInt("StackUpgradeCount");
        if (nbt.contains("Item", Constants.NBT.TAG_COMPOUND))
            itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.read(nbt.getCompound("Item")))).createStack();
        if(nbt.contains("LockedTicks", Constants.NBT.TAG_INT)) {
            lockedTicks = nbt.getByte("LockedTicks");
            this.isLocked = lockedTicks > 0;
        }
        if(speedUpgradeCount > UpgradeType.SPEED.getMax()) speedUpgradeCount = UpgradeType.SPEED.getMax();
        if(stackUpgradeCount > UpgradeType.STACK.getMax()) stackUpgradeCount = UpgradeType.STACK.getMax();
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putInt("SpeedUpgradeCount", speedUpgradeCount);
        nbt.putInt("StackUpgradeCount", stackUpgradeCount);
        nbt.put("Item", itemStack.serializeNBT());
        nbt.putInt("LockedTicks", lockedTicks);
        return nbt;
    }

    public int getGenTime() {
        int time = TIME_BASE - (speedUpgradeCount * 10);
        return Math.max(time, 1);
    }

    public int getStackLimit() {
        // final star shards are too op to allow generating any more than 1
        return itemStack.getItem() == Items.FINAL_STAR_SHARD.get() ? 1 : stackUpgradeCount == 0 ? 1 : (int) Math.pow(2, stackUpgradeCount);
    }

    public int getLockedTime() {
        return !isLocked ? 0 : getGenTime() - lockedTicks;
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) return;
        if(isLocked) lockedTicks++;
        verifyLock();
    }

    /*********
     * Items *
     *********/

    @Override
    public int getSlots() {
        return 4;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        switch (slot) {
            case SPEED_UPGRADE_SLOT: return speedUpgradeCount == 0 ? ItemStack.EMPTY : new ItemStack(Items.SPEED_UPGRADE.get(), speedUpgradeCount);
            case STACK_UPGRADE_SLOT: return stackUpgradeCount == 0 ? ItemStack.EMPTY : new ItemStack(Items.STACK_UPGRADE.get(), stackUpgradeCount);
            case INPUT_SLOT: return itemStack;
            case OUTPUT_SLOT: return verifyLock() ? ItemStack.EMPTY : new ItemStack(itemStack.getItem(), getStackLimit());
            default: return ItemStack.EMPTY;
        }
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    public void lock() {
        isLocked = true;
        lockedTicks = 0;
        markDirty();
    }

    public boolean verifyLock() {
        if(isLocked && lockedTicks >= getGenTime()) {
            isLocked = false;
            lockedTicks = 0;
            markDirty();
        }
        return isLocked;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if(slot != OUTPUT_SLOT || verifyLock() || itemStack.isEmpty()) return ItemStack.EMPTY;
        if(!simulate) lock();
        int limit = getStackLimit();
        return new ItemStack(itemStack.getItem(), Math.min(amount, limit));
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SPEED_UPGRADE_SLOT ? UpgradeType.SPEED.getMax() : slot == STACK_UPGRADE_SLOT ? UpgradeType.STACK.getMax() : slot == INPUT_SLOT ? 1 : getStackLimit();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return
            slot == SPEED_UPGRADE_SLOT ? itemStack.getItem() == Items.SPEED_UPGRADE.get() :
                slot == STACK_UPGRADE_SLOT ? itemStack.getItem() == Items.STACK_UPGRADE.get() :
                    Arrays.stream(Matter.VALUES).anyMatch((m) -> m.getItem() == stack.getItem());
    }

    /****************
     * Capabilities *
     ****************/

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        itemHandlerCapability.invalidate();
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
        return new ContainerMatterReplicator(id, playerInventory, pos, data);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("block.projectexpansion.matter_replicator");
    }
}
