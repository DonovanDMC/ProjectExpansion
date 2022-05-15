package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.nbt.NBTManager;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class TileTransmutationInterface extends TileEntity implements IItemHandler, ITickableTileEntity {
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    private ItemInfo[] info;

    public TileTransmutationInterface() {
        super(TileEntityTypes.TRANSMUTATION_INTERFACE.get());
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId("Owner")) owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING)) ownerName = nbt.getString("OwnerName");
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", owner);
        nbt.putString("OwnerName", ownerName);
        return nbt;
    }

    public void setOwner(PlayerEntity player) {
        owner = player.getUniqueID();
        ownerName = player.getScoreboardName();
        markDirty();
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
    }

    private ItemInfo[] fetchKnowledge() {
        if (info != null) return info;
        return info = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner).getKnowledge().toArray(new ItemInfo[0]);
    }

    private int getMaxCount(int slot) {
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger playerEmc = provider.getEmc();
        if (playerEmc.compareTo(BigInteger.ZERO) < 1) return 0;
        BigInteger targetItemEmc = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(fetchKnowledge()[slot]));
        if (targetItemEmc.compareTo(BigInteger.ZERO) < 1) return 0;
        return playerEmc.divide(targetItemEmc).min(BigInteger.valueOf(Math.max(1, Config.transmutationInterfaceItemCount.get()))).intValue();
    }

    @Override
    public void tick() {
        info = null;
    }

    @Override
    public int getSlots() {
        return fetchKnowledge().length + 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (owner == null || Util.getPlayer(owner) == null) return ItemStack.EMPTY;
        fetchKnowledge();

        if (slot <= 0 || info.length < slot) return ItemStack.EMPTY;
        int maxCount = getMaxCount(slot - 1);
        if (maxCount <= 0) return ItemStack.EMPTY;

        ItemStack item = info[slot - 1].createStack();
        item.setCount(maxCount);
        return item;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot != 0 || owner == null || !isItemValid(slot, stack) || stack.isEmpty() || Util.getPlayer(owner) == null) return stack;
        fetchKnowledge();

        ItemInfo info = ItemInfo.fromStack(stack);
        if (!NBTManager.getPersistentInfo(info).equals(info)) return stack;
        stack = stack.copy();
        int count = stack.getCount();
        stack.setCount(1);

        if (count <= 0) return stack;
        if (simulate) return ItemStack.EMPTY;

        long emcValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
        provider.setEmc(provider.getEmc().add(totalEmcValue));

        ServerPlayerEntity player = Util.getPlayer(world, owner);
        if (player != null) {
            if (provider.addKnowledge(stack)) {
                provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
            }
            provider.syncEmc(player);
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot <= 0 || owner == null || info.length < slot || Util.getPlayer(owner) == null) return ItemStack.EMPTY;
        fetchKnowledge();

        amount = Math.min(amount, getMaxCount(slot - 1));

        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack item = info[slot - 1].createStack();
        item.setCount(amount);

        if (simulate) return item;
        long emcValue = ProjectEAPI.getEMCProxy().getValue(info[slot - 1]);
        BigInteger totalEmcCost = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(amount));
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        provider.setEmc(provider.getEmc().subtract(totalEmcCost));
        ServerPlayerEntity player = Util.getPlayer(world, owner);
        if (player != null) provider.syncEmc(player);

        return item;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return ProjectEAPI.getEMCProxy().hasValue(stack);
    }

    /****************
     * Capabilities *
     ****************/

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        itemHandlerCapability.invalidate();
    }
}
