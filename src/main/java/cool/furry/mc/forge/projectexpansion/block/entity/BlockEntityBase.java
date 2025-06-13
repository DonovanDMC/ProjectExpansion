package cool.furry.mc.neoforge.projectexpansion.block.entity;

import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

public class BlockEntityBase extends BlockEntity {
    private boolean updateComparators;
    public BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
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
     * Dirty *
     *********/

    public void markDirty() {
        if (level != null) {
            markDirty(level, worldPosition);
        }
    }

    public void markDirty(@NotNull Level level, @NotNull BlockPos pos) {
        markDirty(level, pos, false);
    }

    public void markDirty(@NotNull Level level, @NotNull BlockPos pos, boolean recheckComparators) {
        Util.markDirty(level, pos);
        if (recheckComparators && !level.isClientSide) {
            updateComparators = true;
        }
    }

    /********
     * Data *
     ********/

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected class StackHandler extends ItemStackHandler {

        protected StackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }

    protected class CompactableStackHandler extends StackHandler {

        //Start as needing to check for compacting when loaded
        private boolean needsCompacting = true;
        private boolean empty;

        protected CompactableStackHandler(int size) {
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
