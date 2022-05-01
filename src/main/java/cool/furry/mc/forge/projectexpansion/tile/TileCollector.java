package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.gameObjs.tiles.RelayMK1Tile;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileCollector extends TileEntity implements ITickableTileEntity, IEmcStorage {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    public long emc = 0L;

    public TileCollector() {
        super(TileEntityTypes.COLLECTOR.get());
    }

    @Override
    public void read(@Nonnull CompoundNBT nbt) {
        super.read(nbt);
        if (nbt.contains("EMC", Constants.NBT.TAG_LONG)) emc = nbt.getLong(("EMC"));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putLong("EMC", emc);
        return nbt;
    }

    @Override
    public void tick() {
        // we can't use the user defined value due to emc duplication possibilities
        if (world == null || world.isRemote || (world.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        emc += ((BlockCollector) getBlockState().getBlock()).getMatter().getCollectorOutputForTicks(Config.tickDelay.get());
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            TileEntity tile = world.getTileEntity(pos.offset(dir));
            @Nullable IEmcStorage storage = tile == null ? null : tile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).orElse(null);
            if (storage != null && storage.insertEmc(1L, EmcAction.SIMULATE) > 0L) {
                temp.add(storage);

                if (tile instanceof RelayMK1Tile) {
                    for (int i = 0; i < 20; i++) ((RelayMK1Tile) tile).addBonus();
                    tile.markDirty();
                } else if (tile instanceof TileRelay) {
                    ((TileRelay) tile).addBonus();
                    tile.markDirty();
                }
            }
        }

        if (!temp.isEmpty() && emc >= temp.size()) {
            long div = emc / temp.size();

            for (IEmcStorage storage : temp) {
                long action = storage.insertEmc(div, EmcAction.EXECUTE);
                if (action > 0L) {
                    emc -= action;
                    markDirty();
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
                (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) ? this.emcStorageCapability.cast() :
                        super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        this.emcStorageCapability.invalidate();
    }
}
