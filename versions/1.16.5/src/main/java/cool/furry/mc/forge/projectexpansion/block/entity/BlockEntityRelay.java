package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityRelay extends BlockEntityEMC implements IHasMatter, IRelayBigInteger {
    public Matter matter;
    public static final Direction[] DIRECTIONS = Direction.values();
    private BigInteger bonusEMC = BigInteger.ZERO;
    public BlockEntityRelay() {
        super(BlockEntityTypes.RELAY.get());
    }

    public static void tickServer(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
        if (blockEntity instanceof BlockEntityRelay) ((BlockEntityRelay) blockEntity).tickServer(level, pos, state, ((BlockEntityRelay) blockEntity));
    }

    public void tickServer(World level, BlockPos pos, BlockState state, BlockEntityRelay blockEntity) {
        // we can't use the user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        sendToAllAcceptors(level, pos, getStoredEmcBigInteger().min(getMatter().getRelayTransfer()));
    }


    @Override
    public Matter getMatter() {
        BlockRelay block = (BlockRelay) getBlockState().getBlock();
        if (block.getMatter() != matter) {
            this.matter = block.getMatter();
        }
        return matter;
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        bonusEMC = new BigInteger(tag.getString(TagNames.BONUS_EMC));
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putString(TagNames.BONUS_EMC, bonusEMC.toString());
    }

    @Override
    public boolean isRelay() {
        return true;
    }

    @Override
    public BigInteger getBonusToAddBigInteger() {
        return getMatter().getRelayBonus();
    }

    @Override
    public void addBonus(World level, BlockPos pos) {
        bonusEMC = bonusEMC.add(getBonusToAddBigInteger());
        if (bonusEMC.compareTo(BigInteger.ONE) >= 0) {
            insertEmcBigInteger(bonusEMC, EmcAction.EXECUTE);
            bonusEMC = BigInteger.ZERO;
        }
        markDirty(level, pos);
    }

    @Override
    protected void serverTick() {
        tickServer(level, worldPosition, getBlockState(), this);
    }
}
