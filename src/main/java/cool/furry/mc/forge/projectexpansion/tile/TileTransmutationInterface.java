package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class TileTransmutationInterface extends TileNBTFilterable implements IItemHandler, ITickableTileEntity {
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    private ItemInfo[] info;

    public TileTransmutationInterface() {
        super(TileEntityTypes.TRANSMUTATION_INTERFACE.get());
    }

    private ItemInfo[] fetchKnowledge() {
        if (info != null) return info;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if(provider == null) {
            return new ItemInfo[]{};
        }
        return info = provider.getKnowledge().toArray(new ItemInfo[0]);
    }

    private int getMaxCount(int slot) {
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if(provider == null) {
            return 0;
        }
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

        int count = stack.getCount();
        stack = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (count <= 0) return stack;

        if(getFilterStatus() && !NBTManager.getPersistentInfo(info).equals(info)) return stack;
        if (simulate) return ItemStack.EMPTY;

        long emcValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if(provider == null) return stack;
        BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
        provider.setEmc(provider.getEmc().add(totalEmcValue));

        ServerPlayerEntity player = Util.getPlayer(level, owner);
        if (player != null) {
            if (provider.addKnowledge(stack)) provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
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
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if(provider == null) return ItemStack.EMPTY;
        provider.setEmc(provider.getEmc().subtract(totalEmcCost));
        ServerPlayerEntity player = Util.getPlayer(level, owner);
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
