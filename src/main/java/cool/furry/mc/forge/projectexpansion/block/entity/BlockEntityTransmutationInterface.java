package cool.furry.mc.neoforge.projectexpansion.block.entity;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityTransmutationInterface extends BlockEntityNBTFilterable {
    private static final ICapabilityProvider<BlockEntityTransmutationInterface, @Nullable Direction, IItemHandler> ITEM_HANDLER_CAPABILITY = (be, side) -> be.getItemHandler();

    private ItemInfo[] info;

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityTypes.TRANSMUTATION_INTERFACE.get(), ITEM_HANDLER_CAPABILITY);
    }

    public BlockEntityTransmutationInterface(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.TRANSMUTATION_INTERFACE.get(), pos, state);
    }

    private ItemHandler getItemHandler() {
        return new ItemHandler();
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
        BigInteger targetItemEmc = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(fetchKnowledge()[slot]));
        if (targetItemEmc.compareTo(BigInteger.ZERO) < 1) return 0;
        return playerEmc.divide(targetItemEmc).min(BigInteger.valueOf(Math.max(1, Config.server.transmutationInterfaceItemCount.get()))).intValue();
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
            if (owner == null || Util.getServerPlayer(owner) == null) return ItemStack.EMPTY;
            fetchKnowledge();

            if (slot <= 0 || info.length < slot) return ItemStack.EMPTY;
            int maxCount = getMaxCount(slot - 1);
            if (maxCount <= 0) return ItemStack.EMPTY;

            ItemStack item = fetchKnowledge()[slot - 1].createStack();
            item.setCount(maxCount);
            return item;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot != 0 || owner == null || !isItemValid(slot, stack) || stack.isEmpty() || Util.getServerPlayer(owner) == null) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);

            int count = stack.getCount();
            if (count <= 0) return stack;
            stack = stack.copyWithCount(1);

            if(getFilterStatus() && !IEMCProxy.INSTANCE.getPersistentInfo(info).equals(info)) return stack;
            if (simulate) return ItemStack.EMPTY;

            long emcValue = IEMCProxy.INSTANCE.getSellValue(stack);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return stack;
            BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
            provider.setEmc(provider.getEmc().add(totalEmcValue));

            ServerPlayer player = Util.getServerPlayer(level, owner);
            if (player != null) {
                if (provider.addKnowledge(stack)) provider.syncKnowledgeChange(player, IEMCProxy.INSTANCE.getPersistentInfo(info), true);
                provider.syncEmc(player);
            }

            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot <= 0 || owner == null || fetchKnowledge().length < slot || Util.getServerPlayer(owner) == null) return ItemStack.EMPTY;
            fetchKnowledge();

            amount = Math.min(amount, getMaxCount(slot - 1));

            if (amount <= 0) return ItemStack.EMPTY;
            ItemStack item = fetchKnowledge()[slot - 1].createStack();
            item.setCount(amount);

            if (simulate) return item;
            long emcValue = IEMCProxy.INSTANCE.getValue(fetchKnowledge()[slot - 1]);
            BigInteger totalEmcCost = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(amount));
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return ItemStack.EMPTY;
            provider.setEmc(provider.getEmc().subtract(totalEmcCost));
            ServerPlayer player = Util.getServerPlayer(level, owner);
            if (player != null) provider.syncEmc(player);

            return item;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && IEMCProxy.INSTANCE.hasValue(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, worldPosition, getBlockState(), this, null);
    }
}
