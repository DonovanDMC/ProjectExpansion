package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.EmcLock;
import cool.furry.mc.projectexpansion.util.Platform;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityTransmutationInterface extends BlockEntityNBTFilterable {
    private ItemInfo[] info;
    public BlockEntityTransmutationInterface(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.TRANSMUTATION_INTERFACE.get(), pos, state);
        this.addCapability(Platform.ITEM_HANDLER, this::getItemHandler);
    }

    private ItemHandler getItemHandler(@Nullable Direction side) {
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
        BigInteger targetItemEmc = BigInteger.valueOf(Platform.emcProxy().getValue(fetchKnowledge()[slot]));
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

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (owner == null || Util.getPlayer(owner) == null) return ItemStack.EMPTY;
            fetchKnowledge();

            if (slot <= 0 || info.length < slot) return ItemStack.EMPTY;
            int maxCount = getMaxCount(slot - 1);
            if (maxCount <= 0) return ItemStack.EMPTY;

            ItemStack item = fetchKnowledge()[slot - 1].createStack();
            item.setCount(maxCount);
            return item;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || owner == null || !isItemValid(slot, stack) || stack.isEmpty() || Util.getPlayer(owner) == null) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);

            int count = stack.getCount();
            if (count <= 0) return stack;
            // the stack passed in is what's given back, so it's left untouched
            ItemStack single = Platform.copyWithCount(stack, 1);

            if(getFilterStatus() && !Platform.emcProxy().getPersistentInfo(info).equals(info)) return stack;
            if (simulate) return ItemStack.EMPTY;

            long emcValue = Platform.emcProxy().getSellValue(single);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return stack;
            BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
            provider.setEmc(provider.getEmc().add(totalEmcValue));

            ServerPlayer player = Util.getPlayer(level, owner);
            if (player != null) {
                if (provider.addKnowledge(single)) provider.syncKnowledgeChange(player, Platform.emcProxy().getPersistentInfo(info), true);
                provider.syncEmc(player);
            }

            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot <= 0 || owner == null || fetchKnowledge().length < slot || Util.getPlayer(owner) == null) return ItemStack.EMPTY;
            fetchKnowledge();

            amount = Math.min(amount, getMaxCount(slot - 1));

            if (amount <= 0) return ItemStack.EMPTY;
            ItemStack item = fetchKnowledge()[slot - 1].createStack();
            item.setCount(amount);

            if (simulate) return item;
            long emcValue = Platform.emcProxy().getValue(fetchKnowledge()[slot - 1]);
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
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && Platform.emcProxy().hasValue(stack) && !EmcLock.isLocked(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) Util.getCapability(BlockEntityTransmutationInterface.class, Platform.ITEM_HANDLER, level, worldPosition).orElseThrow(() -> new IllegalStateException("failed to get ITEM_HANDLER for block entity at " + worldPosition));
    }
}
