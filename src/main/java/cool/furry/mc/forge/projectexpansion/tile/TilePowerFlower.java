package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.PowerFlowerCollector;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class TilePowerFlower extends TileOwnable implements ITickableTileEntity {
    public BigInteger emc = BigInteger.ZERO;

    public TilePowerFlower() {
        super(TileEntityTypes.POWER_FLOWER.get());
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains(NBTNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString((NBTNames.STORED_EMC)));
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
        if (level == null || level.isClientSide || (level.getGameTime() % Config.tickDelay.get()) != Util.mod(hashCode(), Config.tickDelay.get())) return;

        BigInteger res = ((BlockPowerFlower) getBlockState().getBlock()).getMatter().getPowerFlowerOutputForTicks(Config.tickDelay.get());
        ServerPlayerEntity player = Util.getPlayer(level, owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(res));
            emc = BigInteger.ZERO;
        } else {
            emc = emc.add(res);
        }
        Util.markDirty(this);
    }
}
