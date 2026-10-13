package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockEmcStorage;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.projectexpansion.util.Util;
import cool.furry.mc.projectexpansion.util.storage.EmcStorageTier;
import cool.furry.mc.projectexpansion.util.storage.StorageSide;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;

public class BlockEntityEmcStorage extends BlockEntityEMC {
    private final Map<Direction, SideHandler> sides = new EnumMap<>(Direction.class);

    public BlockEntityEmcStorage(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.EMC_STORAGE.get(), pos, state);
        for (Direction side : Direction.values()) sides.put(side, new SideHandler(side));
        // these replace the ones BlockEntityEMC adds, which give the whole block on every side
        addCapability(PECapabilities.EMC_STORAGE_CAPABILITY, this::getHandler);
        addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, this::getHandler);
    }

    // without a side, like the jade & wthit overlays ask, everything is allowed
    // an off side has nothing to connect to, so pipes leave it alone
    private @Nullable IEmcStorageBigInteger getHandler(@Nullable Direction side) {
        if (side == null) return this;
        return getSide(side) == StorageSide.OFF ? null : sides.get(side);
    }

    public EmcStorageTier getTier() {
        return ((BlockEmcStorage) getBlockState().getBlock()).tier;
    }

    public StorageSide getSide(Direction side) {
        return BlockEmcStorage.getSide(getBlockState(), side);
    }

    // read from the tier each time, so a changed config applies to storage already placed
    @Override
    public BigInteger getMaximumEmcBigInteger() {
        return getTier().getCapacity();
    }

    @Override
    protected boolean emcAffectsComparators() {
        return true;
    }

    public int getComparatorSignal() {
        BigInteger stored = getStoredEmcBigInteger();
        if (stored.signum() <= 0) return 0;
        // anything stored gives at least 1, and only a full storage gives 15
        return Math.max(1, stored.multiply(BigInteger.valueOf(14)).divide(getMaximumEmcBigInteger()).intValue() + (hasMaxedEmcBigInteger() ? 1 : 0));
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BlockEntityEmcStorage storage)) return;
        storage.pushOut(level, pos);
        // comparators only read again when told to
        storage.updateComparators(level, pos);
    }

    // sends up to the tier's rate out of the output sides, into whatever will take it
    private void pushOut(Level level, BlockPos pos) {
        BigInteger budget = getTier().getRate().min(getStoredEmcBigInteger());
        for (Direction side : Direction.values()) {
            if (budget.signum() <= 0) return;
            if (getSide(side) != StorageSide.OUTPUT) continue;
            BigInteger moved = insertInto(level, pos.relative(side), side.getOpposite(), budget);
            if (moved.signum() > 0) {
                forceExtractEmcBigInteger(moved, IEmcStorage.EmcAction.EXECUTE);
                budget = budget.subtract(moved);
            }
        }
    }

    private static BigInteger insertInto(Level level, BlockPos pos, Direction side, BigInteger amount) {
        IEmcStorageBigInteger big = Util.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, level, pos, side).orElse(null);
        if (big != null) return big.insertEmcBigInteger(amount, IEmcStorage.EmcAction.EXECUTE);
        IEmcStorage storage = Util.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, level, pos, side).orElse(null);
        if (storage != null) return BigInteger.valueOf(storage.insertEmc(amount.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue(), IEmcStorage.EmcAction.EXECUTE));
        return BigInteger.ZERO;
    }

    // what one side lets through, input sides only take emc in and output sides only let it be taken
    private class SideHandler implements IEmcStorageBigInteger {
        private final Direction side;

        private SideHandler(Direction side) {
            this.side = side;
        }

        @Override
        public BigInteger getStoredEmcBigInteger() {
            return BlockEntityEmcStorage.this.getStoredEmcBigInteger();
        }

        @Override
        public BigInteger getMaximumEmcBigInteger() {
            return BlockEntityEmcStorage.this.getMaximumEmcBigInteger();
        }

        @Override
        public BigInteger extractEmcBigInteger(BigInteger value, EmcAction action) {
            return getSide(side) == StorageSide.OUTPUT ? BlockEntityEmcStorage.this.extractEmcBigInteger(value, action) : BigInteger.ZERO;
        }

        @Override
        public BigInteger insertEmcBigInteger(BigInteger value, EmcAction action) {
            return getSide(side) == StorageSide.INPUT ? BlockEntityEmcStorage.this.insertEmcBigInteger(value, action) : BigInteger.ZERO;
        }
    }
}
