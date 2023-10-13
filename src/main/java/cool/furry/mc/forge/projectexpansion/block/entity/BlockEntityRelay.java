package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BlockEntityRelay extends BlockEntity implements IHasMatter {
    public BigInteger emc = BigInteger.ZERO;
    public Matter matter;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(EMCHandler::new);
    public static final Direction[] DIRECTIONS = Direction.values();
    public BlockEntityRelay(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.RELAY.get(), pos, state);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityRelay be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityRelay blockEntity) {
        // we can't use the user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        BigInteger transfer = ((BlockRelay) getBlockState().getBlock()).getMatter().getRelayTransfer();
        List<IEmcStorage> temp = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            BlockEntity be = level.getBlockEntity(pos.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
            if (be == null) continue;
            be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
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
            long v = Math.min(Util.safeLongValue(BlockEntityRelay.this.emc), emc);
            if (v < 0L) return insertEmc(-v, action);
            else if (action.execute()) BlockEntityRelay.this.emc = BlockEntityRelay.this.emc.subtract(BigInteger.valueOf(v));
            return v;
        }

        @Override
        public long insertEmc(long emc, EmcAction action) {
            long v = Math.min(getMaximumEmc() - Util.safeLongValue(BlockEntityRelay.this.emc), emc);
            if (v < 0L) return extractEmc(-v, action);
            else if (action.execute()) BlockEntityRelay.this.emc = BlockEntityRelay.this.emc.add(BigInteger.valueOf(v));
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
        return (EMCHandler) getCapability(PECapabilities.EMC_STORAGE_CAPABILITY).orElseThrow(NullPointerException::new);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == PECapabilities.EMC_STORAGE_CAPABILITY) ? emcStorageCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        emcStorageCapability.invalidate();
    }
}
