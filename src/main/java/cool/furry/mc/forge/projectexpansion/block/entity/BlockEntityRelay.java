package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BlockEntityRelay extends BlockEntity implements IEmcStorage {
    public long emc = 0L;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    public static final Direction[] DIRECTIONS = Direction.values();
    public BlockEntityRelay(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.RELAY.get(), pos, state);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if(tag.contains("EMC", Tag.TAG_LONG)) emc = tag.getLong(("EMC"));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EMC", emc);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityRelay be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityRelay blockEntity) {
        // we can't use the user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        long transfer = ((BlockRelay) getBlockState().getBlock()).getMatter().getRelayTransfer();
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            BlockEntity be = level.getBlockEntity(pos.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
            if(be == null) continue;
            be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                if (!storage.isRelay() && storage.insertEmc(1L, EmcAction.SIMULATE) > 0L) temp.add(storage);
            });

        }

        if (!temp.isEmpty() && emc >= temp.size()) {
            long div = Math.min(emc / temp.size(), transfer);

            for (IEmcStorage storage : temp) {
                long action = storage.insertEmc(div, EmcAction.EXECUTE);
                if (action > 0L) {
                    emc -= action;
                    Util.markDirty(this);
                    if (emc < div) break;
                }
            }
        }
    }

    @Override
    public long getStoredEmc() {
        return emc;
    }

    @Override
    public long getMaximumEmc() {
        return Long.MAX_VALUE;
    }

    @Override
    public long extractEmc(long emc, EmcAction action) {
        long v = Math.min(this.emc, emc);

        if (v < 0L) return insertEmc(-v, action);
        else if (action.execute()) this.emc -= v;

        return v;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        long v = Math.min(getMaximumEmc() - this.emc, emc);

        if (v < 0L) return extractEmc(-v, action);
        else if (action.execute()) this.emc += v;

        return v;
    }

    @Override
    public boolean isRelay() {
        return true;
    }

    public void addBonus() {
        if (getBlockState().getBlock() instanceof BlockRelay)
            insertEmc(((BlockRelay) getBlockState().getBlock()).getMatter().getRelayBonus(), EmcAction.EXECUTE);
    }

    /****************
     * Capabilities *
     ****************/

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
                (cap == PECapabilities.EMC_STORAGE_CAPABILITY) ? this.emcStorageCapability.cast() :
                        super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        this.emcStorageCapability.invalidate();
    }
}
