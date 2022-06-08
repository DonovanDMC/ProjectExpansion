package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
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

public class TileRelay extends TileEntity implements ITickableTileEntity, IEmcStorage {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    public BigInteger emc = BigInteger.ZERO;

    public TileRelay() {
        super(TileEntityTypes.RELAY.get());
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.contains(NBTNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString(NBTNames.STORED_EMC));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putString(NBTNames.STORED_EMC, emc.toString());
        return nbt;
    }

    @Override
    public void tick() {
        // we can't use the user defined value due to emc duplication possibilities
        if (world == null || world.isRemote || (world.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        BigInteger transfer = ((BlockRelay) getBlockState().getBlock()).getMatter().getRelayTransfer();
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            TileEntity tile = world.getTileEntity(pos.offset(dir));
            if (tile == null) continue;
            tile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                if (!storage.isRelay() && storage.insertEmc(1L, EmcAction.SIMULATE) > 0L) temp.add(storage);
            });
        }

        emc = Util.spreadEMC(emc, temp, Util.safeLongValue(transfer));
    }

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
        long v = Math.min(Util.safeLongValue(this.emc), emc);

        if (v < 0L) return insertEmc(-v, action);
        else if (action.execute()) this.emc = this.emc.subtract(BigInteger.valueOf(v));

        return v;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        long v = Math.min(getMaximumEmc() - Util.safeLongValue(this.emc), emc);

        if (v < 0L) return extractEmc(-v, action);
        else if (action.execute()) this.emc = this.emc.add(BigInteger.valueOf(v));

        return v;
    }

    @Override
    public boolean isRelay() {
        return true;
    }

    public void addBonus() {
        if (getBlockState().getBlock() instanceof BlockRelay) Util.stepBigInteger(((BlockRelay) getBlockState().getBlock()).getMatter().getRelayBonus(), (val) -> insertEmc(val, EmcAction.EXECUTE));
    }

    /****************
     * Capabilities *
     ****************/

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
