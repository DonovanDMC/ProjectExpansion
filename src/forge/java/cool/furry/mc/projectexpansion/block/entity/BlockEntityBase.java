package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

public class BlockEntityBase extends BlockEntity {
    protected HashMap<Capability<?>, ICapabilityResolver<?>> peCapabilities = new HashMap<>();
    protected HashMap<Capability<?>, Function<Direction, ?>> capabilities = new HashMap<>();
    private boolean updateComparators;
    public BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

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

    protected void updateComparators(Level level, BlockPos pos) {
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
    public void load(CompoundTag tag) {
        super.load(tag);
        this.loadData(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.saveData(tag);
    }

    protected void saveData(CompoundTag tag) {}
    protected void loadData(CompoundTag tag) {}

    /*********
     * Dirty *
     *********/

    public void markDirty() {
        if (level != null) {
            markDirty(level, worldPosition);
        }
    }

    public void markDirty(Level level, BlockPos pos) {
        markDirty(level, pos, false);
    }

    public void markDirty(Level level, BlockPos pos, boolean recheckComparators) {
        Util.markDirty(level, pos);
        if (recheckComparators && !level.isClientSide) {
            updateComparators = true;
        }
    }

    /********
     * Data *
     ********/

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
