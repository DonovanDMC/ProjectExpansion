package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
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
public class BlockEntityCollector extends BlockEntity implements IEmcStorage {
    public long emc = 0L;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    public static final Direction[] DIRECTIONS = Direction.values();
    public BlockEntityCollector(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.ENERGY_COLLECTOR.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if(tag.contains("EMC", Tag.TAG_LONG)) emc = tag.getLong(("EMC"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EMC", emc);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityCollector blockEntity) {
        // we can't use a user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        emc += ((BlockCollector) getBlockState().getBlock()).getMatter().getCollectorOutputForTicks(Config.tickDelay.get());
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            BlockEntity be = level.getBlockEntity(pos.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
            if(be == null) continue;
            be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                if (storage.insertEmc(1L, EmcAction.SIMULATE) > 0L) {
                    temp.add(storage);
                    if (be instanceof RelayMK1BlockEntity) {
                        for (int i = 0; i < 20; i++) ((RelayMK1BlockEntity) be).addBonus();
                        Util.markDirty(be);
                    } else if (be instanceof BlockEntityRelay) {
                        ((BlockEntityRelay) be).addBonus();
                        Util.markDirty(be);
                    }
                }
            });
        }

        if (!temp.isEmpty() && emc >= temp.size()) {
            long div = emc / temp.size();

            for (IEmcStorage storage : temp) {
                long action = storage.insertEmc(div, EmcAction.EXECUTE);
                if (action > 0L) {
                    emc -= action;
                    Util.markDirty(level, this);
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
        long change = Math.min(this.emc, emc);

        if (change < 0L) return insertEmc(-change, action);
        else if (action.execute()) this.emc -= change;

        return change;
    }

    @Override
    public long insertEmc(long l, EmcAction emcAction) {
        return 0L;
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
