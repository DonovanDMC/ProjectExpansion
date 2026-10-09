package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityRelay extends BlockEntityEMC implements IHasMatter, IRelayBigInteger {
    public Matter matter;
    public static final Direction[] DIRECTIONS = Direction.values();
    private BigInteger bonusEMC = BigInteger.ZERO;
    public BlockEntityRelay(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.RELAY.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityRelay be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityRelay blockEntity) {
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
    protected void loadData(CompoundTag tag) {
        super.loadData(tag);
        bonusEMC = new BigInteger(tag.getString(TagNames.BONUS_EMC));
    }

    @Override
    protected void saveData(CompoundTag tag) {
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
    public void addBonus(Level level, BlockPos pos) {
        bonusEMC = bonusEMC.add(getBonusToAddBigInteger());
        if (bonusEMC.compareTo(BigInteger.ONE) >= 0) {
            insertEmcBigInteger(bonusEMC, EmcAction.EXECUTE);
            bonusEMC = BigInteger.ZERO;
        }
        markDirty(level, pos);
    }
}
