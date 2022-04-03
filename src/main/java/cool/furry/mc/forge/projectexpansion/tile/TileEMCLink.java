package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class TileEMCLink extends TileEntity implements ITickableTileEntity, IEmcStorage {
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    public BigInteger emc = BigInteger.ZERO;
    public int tick = 0;
    private LazyOptional<IEmcStorage> emcStorageCapability;

    public TileEMCLink() {
        super(TileEntityTypes.EMC_LINK.get());
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId("Owner")) this.owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING)) this.ownerName = nbt.getString("OwnerName");
        if (nbt.contains("Tick", Constants.NBT.TAG_BYTE)) tick = nbt.getByte("Tick") & 0xFF;
        if (nbt.contains("EMC", Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString(("EMC")));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", this.owner);
        nbt.putString("OwnerName", this.ownerName);
        nbt.putByte("Tick", (byte) tick);
        nbt.putString("EMC", emc.toString());
        return nbt;
    }

    @Override
    public long getStoredEmc() {
        return 0L;
    }

    @Override
    public long getMaximumEmc() {
        return Long.MAX_VALUE;
    }

    @Override
    public long extractEmc(long emc, EmcAction action) {
        return emc < 0L ? insertEmc(-emc, action) : 0L;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        if (emc > 0L) {
            if (action.execute()) this.emc = this.emc.add(BigInteger.valueOf(emc));

            return emc;
        }

        return 0L;
    }

    @Override
    public void tick() {

        if (Util.isWorldRemoteOrNull(getWorld())) return;
        tick++;
        if (tick >= Config.tickDelay.get()) {
            tick = 0;
            if (emc.equals(BigInteger.ZERO)) return;
            ServerPlayerEntity player = Objects.requireNonNull(getWorld().getServer()).getPlayerList().getPlayerByUUID(owner);
            IKnowledgeProvider provider = player == null ? null : player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).orElse(null);

            if (provider != null) {
                provider.setEmc(provider.getEmc().add(emc));
                markDirty();
                emc = BigInteger.ZERO;
                provider.syncEmc(player);
            }
        }
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUniqueID();
        this.ownerName = player.getScoreboardName();
        markDirty();
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) {
            if (emcStorageCapability == null || !emcStorageCapability.isPresent()) {
                emcStorageCapability = LazyOptional.of(() -> this);
            }

            return emcStorageCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();

        if (emcStorageCapability != null && emcStorageCapability.isPresent()) {
            emcStorageCapability.invalidate();
            emcStorageCapability = null;
        }
    }
}
