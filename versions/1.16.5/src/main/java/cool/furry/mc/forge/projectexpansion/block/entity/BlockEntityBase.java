package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

public class BlockEntityBase extends TileEntity implements ITickableTileEntity {
    protected HashMap<Capability<?>, ICapabilityResolver<?>> peCapabilities = new HashMap<>();
    protected HashMap<Capability<?>, Function<Direction, ?>> capabilities = new HashMap<>();
    private boolean updateComparators;
    public BlockEntityBase(TileEntityType<?> type) {
        super(type);
    }

    // block entity tickers were added in 1.17, before that block entities tick themselves
    @Override
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) clientTick();
        else serverTick();
    }

    protected void serverTick() {}
    protected void clientTick() {}

    /***************
     * Capabilities *
     ***************/

    protected <CAPABILITY> void addPECapability(Capability<CAPABILITY> capability, ICapabilityResolver<CAPABILITY> resolver) {
        this.peCapabilities.put(capability, resolver);
    }

    protected <CAPABILITY> void addCapability(Capability<CAPABILITY> capability, Function<Direction, CAPABILITY> getter) {
        this.capabilities.put(capability, getter);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (this.capabilities.containsKey(cap)) {
            Object value = this.capabilities.get(cap).apply(side);
            //noinspection unchecked
            return value == null ? LazyOptional.empty() : LazyOptional.of(() -> (T) value);
        }

        if (this.peCapabilities.containsKey(cap)) {
            return this.peCapabilities.get(cap).getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.peCapabilities.values().forEach(ICapabilityResolver::invalidateAll);
    }

    /***************
     * Comparators *
     ***************/

    protected void updateComparators(World level, BlockPos pos) {
        //Only update the comparator state if we need to update comparators
        //Note: We call this at the end of child implementations to try and update any changes immediately instead
        // of them having to be delayed a tick
        if (updateComparators) {
            BlockState state = getBlockState();
            if (!state.isAir()) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            updateComparators = false;
        }
    }

    @Override
    public final void setChanged() {
        if (level != null) {
            markDirty(level, worldPosition, true);
        }
    }

    /*********
     * Data *
     *********/

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        this.loadData(tag);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        this.saveData(tag);
        return tag;
    }

    protected void saveData(CompoundNBT tag) {}
    protected void loadData(CompoundNBT tag) {}

    /*********
     * Dirty *
     *********/

    public void markDirty() {
        if (level != null) {
            markDirty(level, worldPosition);
        }
    }

    public void markDirty(World level, BlockPos pos) {
        markDirty(level, pos, false);
    }

    public void markDirty(World level, BlockPos pos, boolean recheckComparators) {
        Util.markDirty(level, pos);
        if (recheckComparators && !level.isClientSide) {
            updateComparators = true;
        }
    }

    /********
     * Data *
     ********/

    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Override
    public final SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, -1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        load(getBlockState(), packet.getTag());
    }

    protected class StackHandler extends ItemStackHandler {

        public StackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }

    @SuppressWarnings("unused")
    protected class CompactableStackHandler extends StackHandler {

        //Start as needing to check for compacting when loaded
        private boolean needsCompacting = true;
        private boolean empty;

        public CompactableStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            needsCompacting = true;
        }

        public void compact() {
            if (needsCompacting) {
                if (level != null && !level.isClientSide) {
                    empty = ItemHelper.compactInventory(this);
                }
                needsCompacting = false;
            }
        }

        @Override
        protected void onLoad() {
            super.onLoad();
            empty = IntStream.range(0, getSlots()).allMatch(slot -> getStackInSlot(slot).isEmpty());
        }

        /**
         * @apiNote Only use this on the server
         */
        public boolean isEmpty() {
            return empty;
        }
    }
}
