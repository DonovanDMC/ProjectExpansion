package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TileCollector extends TileEntity implements ITickableTileEntity, IHasMatter {
    public BigInteger emc = BigInteger.ZERO;
    public Matter matter;
    public static final Direction[] DIRECTIONS = Direction.values();
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(EMCHandler::new);

    public TileCollector() {
        super(TileEntityTypes.COLLECTOR.get());
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains(NBTNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString(NBTNames.STORED_EMC));
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        super.save(nbt);
        nbt.putString(NBTNames.STORED_EMC, emc.toString());
        return nbt;
    }

    @Override
    public void tick() {
        // we can't use a user defined value due to emc duplication possibilities
        if (level == null || level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        emc = emc.add(((BlockCollector) getBlockState().getBlock()).getMatter().getCollectorOutputForTicks(Config.tickDelay.get()));
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            TileEntity tile = level.getBlockEntity(worldPosition.offset(dir.getNormal()));
            if(tile == null) continue;
            tile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                if (storage.insertEmc(1L, IEmcStorage.EmcAction.SIMULATE) > 0L) {
                    temp.add(storage);
                    if (tile instanceof RelayMK1Tile) {
                        for (int i = 0; i < 20; i++) ((RelayMK1Tile) tile).addBonus();
                    } else if (tile instanceof TileRelay) {
                        ((TileRelay) tile).getEMCHandlerCapability().addBonus();
                    }
                    Util.markDirty(this);
                }
            });
        }

        emc = Util.spreadEMC(emc, temp);
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        if (level != null) {
            BlockCollector block = (BlockCollector) getBlockState().getBlock();
            if (block.getMatter() != matter) setMatter(block.getMatter());
            return matter;
        }
        return Matter.BASIC;
    }

    private void setMatter(Matter matter) {
        this.matter = matter;
    }


    /****************
     * Capabilities *
     ****************/

    class EMCHandler implements IEmcStorage {
        @Override
        public long getStoredEmc() {
            return Util.safeLongValue(emc);
        }

        @Override
        public long getMaximumEmc() {
            return Long.MAX_VALUE;
        }

        @Override
        public long extractEmc(long emc, EmcAction action) {
            long change = Math.min(Util.safeLongValue(TileCollector.this.emc), emc);
            if (change < 0L) return insertEmc(-change, action);
            else if (action.execute()) {
                TileCollector.this.emc = TileCollector.this.emc.subtract(BigInteger.valueOf(change));
                Util.markDirty(TileCollector.this);
            }
            return change;
        }

        @Override
        public long insertEmc(long l, EmcAction emcAction) {
            return 0L;
        }

    }

    EMCHandler getEMCHandlerCapability() {
        return (EMCHandler) getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY).orElseThrow(NullPointerException::new);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) ? emcStorageCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        emcStorageCapability.invalidate();
    }
}
