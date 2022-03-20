package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.gameObjs.tiles.RelayMK1Tile;
import net.minecraft.block.BlockState;
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
    public long emc = 0L;
    public int tick = 0;
    private LazyOptional<IEmcStorage> emcStorageCapability;
    public static final Direction[] DIRECTIONS = Direction.values();
    public TileCollector() {
        super(TileEntityTypes.ENERGY_COLLECTOR.get());
    }

    @Override
    public void read(@Nonnull CompoundNBT nbt) {
        super.read(nbt);
        if(nbt.contains("Tick", Constants.NBT.TAG_BYTE)) tick = nbt.getByte("Tick") & 0xFF;
        if(nbt.contains("EMC", Constants.NBT.TAG_LONG)) emc = nbt.getLong(("EMC"));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putByte("Tick", (byte) tick);
        nbt.putLong("EMC", emc);
        return nbt;
    }

    @Override
    public void tick() {
        if(world == null || world.isRemote()) return;
        tick++;

        // we can't use a user defined value due to emc duplication possibilities
        if(tick >= 20) {
            tick = 0;
            emc += ((BlockCollector) getBlockState().getBlock()).getMatter().getCollectorOutputForTicks(Config.tickDelay.get());
            List<IEmcStorage> temp = new ArrayList<>(1);

            for (Direction dir : DIRECTIONS) {
                TileEntity tile = world.getTileEntity(pos.offset(dir));
                @Nullable IEmcStorage storage = tile == null ? null : tile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).orElse(null);
                if(storage != null && storage.insertEmc(1L, EmcAction.SIMULATE) > 0L) {
                    temp.add(storage);

                    if(tile instanceof RelayMK1Tile) {
                        for (int i = 0; i < 20; i++) {
                            ((RelayMK1Tile) tile).addBonus();
                        }
                        tile.markDirty();
                    } else if (tile instanceof TileRelay) {
                        ((TileRelay) tile).addBonus();
                        tile.markDirty();
                    }
                }
            }

            if(!temp.isEmpty() && emc >= temp.size()) {
                long div = emc / temp.size();

                for(IEmcStorage storage : temp) {
                    long action = storage.insertEmc(div, EmcAction.EXECUTE);
                    if(action > 0L) {
                        emc -= action;
                        markDirty();
                        if(emc < div) break;
                    }
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

        if (change < 0L) {
            return insertEmc(-change, action);
        } else if (action.execute()) {
            this.emc -= change;
        }

        return change;
    }

    @Override
    public long insertEmc(long l, EmcAction emcAction) {
        return 0L;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) {
            if (emcStorageCapability == null || !emcStorageCapability.isPresent()) {
                emcStorageCapability = LazyOptional.of(() -> this);
            }

            return emcStorageCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();

        if (emcStorageCapability != null && emcStorageCapability.isPresent()) {
            emcStorageCapability.invalidate();
            emcStorageCapability = null;
        }
    }
}
