package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.PowerFlowerCollector;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class TilePowerFlower extends TileEntity implements ITickableTileEntity {
    public BigInteger emc = BigInteger.ZERO;
    public UUID owner = new UUID(0L, 0L);
    public String ownerName = "";

    public TilePowerFlower() {
        super(Objects.requireNonNull(TileEntityTypes.POWER_FLOWER.get()));
    }

    @Override
    public void read(@Nonnull CompoundNBT nbt) {
        super.read(nbt);
        if (nbt.hasUniqueId("Owner"))
            owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING))
            ownerName = nbt.getString("OwnerName");
        if (nbt.contains(moze_intel.projecte.utils.Constants.NBT_KEY_STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString((moze_intel.projecte.utils.Constants.NBT_KEY_STORED_EMC)));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", owner);
        nbt.putString("OwnerName", ownerName);
        nbt.putString(moze_intel.projecte.utils.Constants.NBT_KEY_STORED_EMC, emc.toString());
        return nbt;
    }

    public void setOwner(PlayerEntity player) {
        owner = player.getUniqueID();
        ownerName = player.getScoreboardName();
        markDirty();
    }

    @SuppressWarnings("unused")
    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity)
            setOwner((PlayerEntity) livingEntity);
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote || (world.getGameTime() % Config.tickDelay.get()) != Util.mod(hashCode(), Config.tickDelay.get())) return;
        BigInteger res = ((BlockPowerFlower) getBlockState().getBlock()).getMatter().getPowerFlowerOutputForTicks(Config.tickDelay.get());
        ServerPlayerEntity player = Util.getPlayer(world, owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(res));
            emc = BigInteger.ZERO;
            markDirty();
        } else {
            emc = emc.add(res);
            markDirty();
        }
    }
}
