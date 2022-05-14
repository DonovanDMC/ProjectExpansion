package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

@SuppressWarnings("unused")
public class BlockEntityTransmutationInterface extends BlockEntity implements IItemHandler {
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);

    private ItemInfo[] info;

    public BlockEntityTransmutationInterface(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.TRANSMUTATION_INTERFACE.get(), pos, state);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner"))
            owner = tag.getUUID("Owner");
        if (tag.contains("OwnerName", Tag.TAG_STRING))
            ownerName = tag.getString("OwnerName");
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }

    @SuppressWarnings("unused")
    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof Player player)
            setOwner(player);
    }

    private ItemInfo[] fetchKnowledge() {
        if (info != null)
            return info;
        return info = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner).getKnowledge().toArray(new ItemInfo[0]);
    }

    private int getMaxCount(int slot) {
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger playerEmc = provider.getEmc();
        if (playerEmc.compareTo(BigInteger.ZERO) < 1)
            return 0;
        BigInteger targetItemEmc = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(fetchKnowledge()[slot]));
        if (targetItemEmc.compareTo(BigInteger.ZERO) < 1)
            return 0;
        return playerEmc.divide(targetItemEmc).min(BigInteger.valueOf(Integer.MAX_VALUE)).intValueExact();
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityTransmutationInterface be)
            be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityTransmutationInterface blockEntity) {
        info = null;
    }

    @Override
    public int getSlots() {
        return fetchKnowledge().length + 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (owner == null || Util.getPlayer(owner) == null)
            return ItemStack.EMPTY;
        fetchKnowledge();

        if (slot <= 0 || info.length < slot)
            return ItemStack.EMPTY;
        int maxCount = getMaxCount(slot - 1);
        if (maxCount <= 0)
            return ItemStack.EMPTY;

        ItemStack item = info[slot - 1].createStack();
        item.setCount(maxCount);
        return item;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot != 0 || owner == null || !isItemValid(slot, stack) || stack.isEmpty() || Util.getPlayer(owner) == null)
            return stack;
        fetchKnowledge();

        ItemInfo info = ItemInfo.fromStack(stack);
        if (!NBTManager.getPersistentInfo(info).equals(info))
            return stack;
        stack = stack.copy();
        int count = stack.getCount();
        stack.setCount(1);

        if (count <= 0)
            return stack;
        if (simulate)
            return ItemStack.EMPTY;

        long emcValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger totalEmcValue = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(count));
        provider.setEmc(provider.getEmc().add(totalEmcValue));

        ServerPlayer player = Util.getPlayer(level, owner);
        if (player != null) {
            if (provider.addKnowledge(stack))
                provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
            provider.syncEmc(player);
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot <= 0 || owner == null || info.length < slot || Util.getPlayer(owner) == null)
            return ItemStack.EMPTY;
        fetchKnowledge();

        amount = Math.min(amount, getMaxCount(slot - 1));

        if (amount <= 0)
            return ItemStack.EMPTY;
        ItemStack item = info[slot - 1].createStack();
        item.setCount(amount);

        if (simulate)
            return item;
        long emcValue = ProjectEAPI.getEMCProxy().getValue(info[slot - 1]);
        BigInteger totalEmcCost = BigInteger.valueOf(emcValue).multiply(BigInteger.valueOf(amount));
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        provider.setEmc(provider.getEmc().subtract(totalEmcCost));
        ServerPlayer player = Util.getPlayer(level, owner);
        if (player != null)
            provider.syncEmc(player);

        return item;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Math.min(1, Config.transmutationInterfaceItemCount.get());
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
    public void invalidateCaps() {
        itemHandlerCapability.invalidate();
    }
}
