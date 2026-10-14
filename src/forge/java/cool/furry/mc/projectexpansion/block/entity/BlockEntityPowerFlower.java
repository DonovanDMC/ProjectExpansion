package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntityOwnable implements IHasMatter, IHasSunBonus, IEmcStorageBigInteger, IGeneratesEMC {
    public BigInteger emc = BigInteger.ZERO;
    private PowerFlowerMode mode = PowerFlowerMode.PLAYER;
    public BlockEntityPowerFlower(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_FLOWER.get(), pos, state);
        addCapability(PECapabilities.EMC_STORAGE_CAPABILITY, this::getStorage);
        addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, this::getStorage);
    }

    @Override
    protected void loadData(CompoundTag tag) {
        super.loadData(tag);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        mode = PowerFlowerMode.byName(tag.getString(TagNames.MODE));
    }

    @Override
    protected void saveData(CompoundTag tag) {
        super.saveData(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.putString(TagNames.MODE, mode.getSerializedName());
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityPowerFlower be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityPowerFlower blockEntity) {
        if (level.isClientSide || (level.getGameTime() % Config.server.tickDelay.get()) != Util.mod(hashCode(), Config.server.tickDelay.get())) return;
        BigInteger res = getMatter().getPowerFlowerOutputForTicks(Config.server.tickDelay.get());
        if(hasSunBonus() && getSunBonus() != null) {
            res = res.multiply(BigInteger.valueOf(getSunBonus()));
        }
        emc = emc.add(res);
        switch (mode) {
            case PLAYER -> {
                ServerPlayer player = Util.getPlayer(level, owner);
                if (player != null) {
                    PowerFlowerCollector.add(player, emc);
                    emc = BigInteger.ZERO;
                }
            }
            case EJECT -> eject(level, pos);
            case STORAGE -> {}
        }
        markDirty();
    }

    // what nothing around it takes stays in the flower
    private void eject(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (emc.signum() <= 0) return;
            emc = emc.subtract(insertInto(level, pos.relative(side), side.getOpposite(), emc));
        }
    }

    private static BigInteger insertInto(Level level, BlockPos pos, Direction side, BigInteger amount) {
        IEmcStorageBigInteger big = Util.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, level, pos, side).orElse(null);
        if (big != null) return big.insertEmcBigInteger(amount, EmcAction.EXECUTE);
        IEmcStorage storage = Util.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, level, pos, side).orElse(null);
        if (storage != null) return BigInteger.valueOf(storage.insertEmc(Util.safeLongValue(amount), EmcAction.EXECUTE));
        return BigInteger.ZERO;
    }

    public PowerFlowerMode getMode() {
        return mode;
    }

    public void setMode(PowerFlowerMode mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        markDirty();
        if (level == null || level.isClientSide) return;
        // pipes only look for something to connect to when a neighbor changes
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    // nothing can take emc that's on its way to the owner
    private @Nullable IEmcStorageBigInteger getStorage(@Nullable Direction side) {
        return mode == PowerFlowerMode.PLAYER ? null : this;
    }

    @Override
    public Matter getMatter() {
        return ((BlockPowerFlower) getBlockState().getBlock()).getMatter();
    }

    @Override
    public boolean hasSunBonus() {
        return BlockCompactSun.adjacent(level, worldPosition, Direction.DOWN);
    }

    @Override
    public BigInteger getStoredEmcBigInteger() {
        return emc;
    }

    @Override
    public BigInteger getMaximumEmcBigInteger() {
        BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
        if(hasSunBonus()) {
            max = max.multiply(BigInteger.valueOf(Objects.requireNonNull(getSunBonus())));
        }
        return max;
    }

    @Override
    public BigInteger extractEmcBigInteger(BigInteger toExtract, EmcAction action) {
        if (toExtract.compareTo(BigInteger.ZERO) < 0) {
            return insertEmcBigInteger(toExtract.negate(), action);
        }
        if (mode == PowerFlowerMode.PLAYER) return BigInteger.ZERO;
        BigInteger extracted = emc.min(toExtract);
        if (action.execute() && extracted.signum() > 0) {
            emc = emc.subtract(extracted);
            markDirty();
        }
        return extracted;
    }

    @Override
    public BigInteger insertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.compareTo(BigInteger.ZERO) < 0) {
            return extractEmcBigInteger(toAccept.negate(), action);
        }
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getGeneratedEMC() {
        BigInteger generated = getMatter().getPowerFlowerOutput();
        if(hasSunBonus() && getSunBonus() != null) {
            generated = generated.multiply(BigInteger.valueOf(getSunBonus()));
        }
        return generated;
    }
}
