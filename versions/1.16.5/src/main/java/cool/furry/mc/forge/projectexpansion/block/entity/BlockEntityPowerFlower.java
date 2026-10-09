package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntityOwnable implements IHasMatter, IHasSunBonus, IEmcStorageBigInteger, IGeneratesEMC {
    public BigInteger emc = BigInteger.ZERO;
    public BlockEntityPowerFlower() {
        super(BlockEntityTypes.POWER_FLOWER.get());
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        if (tag.contains(TagNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
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
        ServerPlayerEntity player = Util.getPlayer(level, owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(res));
            emc = BigInteger.ZERO;
            markDirty();
        } else {
            emc = emc.add(res);
            markDirty();
        }
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
        return BigInteger.ZERO;
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
