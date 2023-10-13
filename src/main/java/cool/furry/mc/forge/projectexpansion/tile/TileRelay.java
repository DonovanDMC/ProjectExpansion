package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
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

public class TileRelay extends TileEntity implements ITickableTileEntity, IHasMatter {
    public BigInteger emc = BigInteger.ZERO;
    public Matter matter;
    public static final Direction[] DIRECTIONS = Direction.values();
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(EMCHandler::new);

    public TileRelay() {
        super(TileEntityTypes.RELAY.get());
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
        // we can't use the user defined value due to emc duplication possibilities
        if (level == null || level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        BigInteger transfer = ((BlockRelay) getBlockState().getBlock()).getMatter().getRelayTransfer();
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            TileEntity tile = level.getBlockEntity(worldPosition.offset(dir.getNormal()));
            if (tile == null) continue;
            tile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                if (!storage.isRelay() && storage.insertEmc(1L, IEmcStorage.EmcAction.SIMULATE) > 0L) temp.add(storage);
            });
        }

        emc = Util.spreadEMC(emc, temp, getMatter() == Matter.FINAL ? null : Util.safeLongValue(transfer));
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        if (level != null) {
            BlockRelay block = (BlockRelay) getBlockState().getBlock();
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
            long v = Math.min(Util.safeLongValue(TileRelay.this.emc), emc);

            if (v < 0L) return insertEmc(-v, action);
            else if (action.execute()) TileRelay.this.emc = TileRelay.this.emc.subtract(BigInteger.valueOf(v));

            return v;
        }

        @Override
        public long insertEmc(long emc, EmcAction action) {
            long v = Math.min(getMaximumEmc() - Util.safeLongValue(TileRelay.this.emc), emc);

            if (v < 0L) return extractEmc(-v, action);
            else if (action.execute()) TileRelay.this.emc = TileRelay.this.emc.add(BigInteger.valueOf(v));

            return v;
        }

        @Override
        public boolean isRelay() {
            return true;
        }

        public void addBonus() {
            if (getBlockState().getBlock() instanceof BlockRelay)
                Util.stepBigInteger(((BlockRelay) getBlockState().getBlock()).getMatter().getRelayBonus(), (val) -> insertEmc(val, EmcAction.EXECUTE));
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
