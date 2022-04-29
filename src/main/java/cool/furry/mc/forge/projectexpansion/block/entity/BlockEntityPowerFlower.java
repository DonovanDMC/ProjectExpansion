package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.PowerFlowerCollector;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
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
import java.util.UUID;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntity {
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    public BigInteger emc = BigInteger.ZERO;
    public BlockEntityPowerFlower(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_FLOWER.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unused")
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if(tag.hasUUID("Owner")) this.owner = tag.getUUID("Owner");
        if(tag.contains("OwnerName", Tag.TAG_STRING)) this.ownerName = tag.getString("OwnerName");
        if(tag.contains("EMC", Tag.TAG_STRING)) emc = new BigInteger(tag.getString(("EMC")));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("Owner", this.owner);
        tag.putString("OwnerName", this.ownerName);
        tag.putString("EMC", emc.toString());
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if(livingEntity instanceof Player player) setOwner(player);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityPowerFlower blockEntity) {
        if(!(state.getBlock() instanceof BlockPowerFlower block)) return;
        if ((level.getGameTime() % Config.tickDelay.get()) != Util.mod(hashCode(), Config.tickDelay.get())) return;
        long res = block.getMatter().getPowerFlowerOutputForTicks(Config.tickDelay.get());
        ServerPlayer player = Util.getPlayer(owner);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(BigInteger.valueOf(res)));
            Util.markDirty(this);
            emc = BigInteger.ZERO;
        } else {
            emc = emc.add(BigInteger.valueOf(res));
            Util.markDirty(this);
        }
    }
}
