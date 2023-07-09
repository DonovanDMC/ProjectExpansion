package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityTransmutationInterface extends BlockEntityNBTFilterable {
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(ItemHandler::new);

    private ItemInfo[] info;

    public BlockEntityTransmutationInterface(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.TRANSMUTATION_INTERFACE.get(), pos, state);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
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

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityTransmutationInterface be)
            be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityTransmutationInterface blockEntity) {
        info = null;
    }

    /****************
     * Capabilities *
     ****************/

    private class ItemHandler implements IItemHandler {
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

            ItemInfo info = ItemInfo.fromStack(stack);

            int count = stack.getCount();
            if (count <= 0) return stack;
            stack = ItemHandlerHelper.copyStackWithSize(stack, 1);

            if(getFilterStatus() && !NBTManager.getPersistentInfo(info).equals(info)) return stack;
            if (simulate) return ItemStack.EMPTY;

            long emcValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return stack;
            BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
            provider.setEmc(provider.getEmc().add(totalEmcValue));

            ServerPlayer player = Util.getPlayer(level, owner);
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
            ServerPlayer player = Util.getPlayer(level, owner);
            if (player != null) provider.syncEmc(player);

            return item;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && ProjectEAPI.getEMCProxy().hasValue(stack);
        }
    }

    public ItemHandler getItemHandlerCapability() {
        return (ItemHandler) getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(NullPointerException::new);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == ForgeCapabilities.ITEM_HANDLER) ? itemHandlerCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        itemHandlerCapability.invalidate();
    }
}
