package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntityOwnable implements IHasMatter, IHasSunBonus, IEmcStorageBigInteger, IGeneratesEMC {
    public BigInteger emc = BigInteger.ZERO;
    private PowerFlowerMode mode = PowerFlowerMode.PLAYER;
    public BlockEntityPowerFlower() {
        super(BlockEntityTypes.POWER_FLOWER.get());
        addCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, this::getStorage);
        addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, this::getStorage);
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        if (tag.contains(TagNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        mode = PowerFlowerMode.byName(tag.getString(TagNames.MODE));
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.putString(TagNames.MODE, mode.getSerializedName());
    }

    public static void tickServer(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
        if (blockEntity instanceof BlockEntityPowerFlower) ((BlockEntityPowerFlower) blockEntity).tickServer(level, pos, state, ((BlockEntityPowerFlower) blockEntity));
    }

    public void tickServer(World level, BlockPos pos, BlockState state, BlockEntityPowerFlower blockEntity) {
        if (level.isClientSide || (level.getGameTime() % Config.server.tickDelay.get()) != Util.mod(hashCode(), Config.server.tickDelay.get())) return;
        BigInteger res = getMatter().getPowerFlowerOutputForTicks(Config.server.tickDelay.get());
        if(hasSunBonus() && getSunBonus() != null) {
            res = res.multiply(BigInteger.valueOf(getSunBonus()));
        }
        emc = emc.add(res);
        if (mode == PowerFlowerMode.PLAYER) {
            ServerPlayerEntity player = Util.getPlayer(level, owner);
            if (player != null) {
                PowerFlowerCollector.add(player, emc);
                emc = BigInteger.ZERO;
            }
        } else if (mode == PowerFlowerMode.EJECT) {
            eject(level, pos);
        }
        markDirty();
    }

    // what nothing around it takes stays in the flower
    private void eject(World level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (emc.signum() <= 0) return;
            emc = emc.subtract(insertInto(level, pos.relative(side), side.getOpposite(), emc));
        }
    }

    private static BigInteger insertInto(World level, BlockPos pos, Direction side, BigInteger amount) {
        TileEntity target = level.getBlockEntity(pos);
        if (target == null) return BigInteger.ZERO;
        IEmcStorageBigInteger big = target.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).orElse(null);
        if (big != null) return big.insertEmcBigInteger(amount, EmcAction.EXECUTE);
        IEmcStorage storage = target.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, side).orElse(null);
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

    @Override
    protected void serverTick() {
        tickServer(level, worldPosition, getBlockState(), this);
    }
}
