package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.EmcLock;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityTransmutationInterface extends BlockEntityNBTFilterable {
    private ItemInfo[] info;
    public BlockEntityTransmutationInterface() {
        super(BlockEntityTypes.TRANSMUTATION_INTERFACE.get());
        this.addCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this::getItemHandler);
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
        BigInteger targetItemEmc = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(fetchKnowledge()[slot]));
        if (targetItemEmc.compareTo(BigInteger.ZERO) < 1) return 0;
        return playerEmc.divide(targetItemEmc).min(BigInteger.valueOf(Math.max(1, Config.server.transmutationInterfaceItemCount.get()))).intValue();
    }

    public static void tickServer(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
        if (blockEntity instanceof BlockEntityTransmutationInterface)
            ((BlockEntityTransmutationInterface) blockEntity).tickServer(level, pos, state, ((BlockEntityTransmutationInterface) blockEntity));
    }

    public void tickServer(World level, BlockPos pos, BlockState state, BlockEntityTransmutationInterface blockEntity) {
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
            stack = ItemHandlerHelper.copyStackWithSize(stack, 1);

            if(getFilterStatus() && !ProjectEAPI.getEMCProxy().getPersistentInfo(info).equals(info)) return stack;
            if (simulate) return ItemStack.EMPTY;

            long emcValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return stack;
            BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
            provider.setEmc(provider.getEmc().add(totalEmcValue));

            ServerPlayerEntity player = Util.getPlayer(level, owner);
            if (player != null) {
                if (provider.addKnowledge(stack)) provider.syncKnowledgeChange(player, ProjectEAPI.getEMCProxy().getPersistentInfo(info), true);
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
            long emcValue = ProjectEAPI.getEMCProxy().getValue(fetchKnowledge()[slot - 1]);
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
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && ProjectEAPI.getEMCProxy().hasValue(stack) && !EmcLock.isLocked(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) Util.getCapability(BlockEntityTransmutationInterface.class, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, level, worldPosition).orElseThrow(() -> new IllegalStateException("failed to get ITEM_HANDLER for block entity at " + worldPosition));
    }

    @Override
    protected void serverTick() {
        tickServer(level, worldPosition, getBlockState(), this);
    }
}
