package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.PowerFlowerCollector;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.utils.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntity {
    public BigInteger emc = BigInteger.ZERO;
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    public BlockEntityPowerFlower(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_FLOWER.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unused")
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner"))
            owner = tag.getUUID("Owner");
        if (tag.contains("OwnerName", Tag.TAG_STRING))
            ownerName = tag.getString("OwnerName");
        if (tag.contains(Constants.NBT_KEY_STORED_EMC, Tag.TAG_STRING))
            emc = new BigInteger(tag.getString((Constants.NBT_KEY_STORED_EMC)));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putString(Constants.NBT_KEY_STORED_EMC, emc.toString());
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof Player player)
            setOwner(player);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityPowerFlower be)
            be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityPowerFlower blockEntity) {
        if (level.isClientSide || (level.getGameTime() % Config.tickDelay.get()) != Util.mod(hashCode(), Config.tickDelay.get()))
            return;
        long res = ((BlockPowerFlower) getBlockState().getBlock()).getMatter().getPowerFlowerOutputForTicks(Config.tickDelay.get());
        ServerPlayer player = Util.getPlayer(level, owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(BigInteger.valueOf(res)));
            emc = BigInteger.ZERO;
            Util.markDirty(this);
        } else {
            emc = emc.add(BigInteger.valueOf(res));
            Util.markDirty(this);
        }
    }
}
