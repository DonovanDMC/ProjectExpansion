package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.container.ContainerEMCExport;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.TileEntityInventoryHelper;
import cool.furry.mc.forge.projectexpansion.util.Util;
import io.netty.buffer.Unpooled;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class TileEMCExport extends TileEntityInventoryHelper implements ITickableTileEntity, INamedContainerProvider, IItemHandler, ICapabilityProvider {
    private static final int[] SLOTS = new int[]{0, 1};
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);


    public TileEMCExport() {
        super(TileEntityTypes.EMC_EXPORT.get(), 2);
    }

    @Nullable
    private ServerPlayerEntity getOwnerPlayer() {
        World world = getWorld();
        if (Util.isWorldRemoteOrNull(getWorld())) return null;
        return (ServerPlayerEntity) world.getPlayerByUuid(owner);
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId("Owner")) this.owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING)) this.ownerName = nbt.getString("OwnerName");
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", this.owner);
        nbt.putString("OwnerName", this.ownerName);
        return nbt;
    }

    private IKnowledgeProvider getProvider() {
        return ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
    }

    private IEMCProxy getEMC() {
        return ProjectEAPI.getEMCProxy();
    }

    // make sure we aren't double charging or doing something else funky, we can still have
    // random race conditions though
    private boolean isRefilling = false;

    private void refill() {
        if (isRefilling) return;
        if (Util.isWorldRemoteOrNull(getWorld())) return;
        isRefilling = true;

        ItemStack input = getStackInSlot(0);
        ItemStack stack = getStackInSlot(1);
        ServerPlayerEntity player = getOwnerPlayer();
        IKnowledgeProvider provider = getProvider();
        if (input.isEmpty()) {
            if (!stack.isEmpty()) {
                getProvider().setEmc(getProvider().getEmc().add(BigInteger.valueOf(getEMC().getValue(stack.getItem()) * stack.getCount())));
                setInventorySlotContents(1, ItemStack.EMPTY);
            }
            isRefilling = false;
            return;

        }

        if (stack.getCount() == input.getMaxStackSize()) {
            isRefilling = false;
            return;
        }
        long cost = getEMC().getValue(input.getItem());
        BigInteger emc = provider.getEmc();
        if (emc.compareTo(BigInteger.ZERO) < 0) emc = BigInteger.ZERO;
        if (emc.subtract(BigInteger.valueOf(cost)).compareTo(BigInteger.ZERO) < 0) {
            isRefilling = false;
            return;
        }
        long maxBuy = emc.divide(BigInteger.valueOf(cost)).longValue();
        int count = 0;
        if (stack.isEmpty()) count = input.getMaxStackSize();
        if (stack.getCount() != input.getMaxStackSize()) count = input.getMaxStackSize() - stack.getCount();

        if (count == 0) {
            isRefilling = false;
            return;
        }
        if (count > maxBuy) count = (int) maxBuy;
        provider.setEmc(emc.subtract(BigInteger.valueOf(count * cost)));
        if (player != null) provider.syncEmc(player);
        setInventorySlotContents(1, new ItemStack(input.getItem(), count + stack.getCount()));
        isRefilling = false;
    }

    @Override
    public void tick() {
        refill();
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
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(getItems(), index);
        refill();
        return stack;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return getEMC().hasValue(stack.getItem());
        else return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return index == 1;
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ContainerEMCExport(windowId, playerInventory, new PacketBuffer(Unpooled.buffer()).writeBlockPos(pos));
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("block.projectexpansion.emc_export");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? this.itemHandlerCapability.cast() : super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        this.itemHandlerCapability.invalidate();
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0) return ItemStack.EMPTY;
        ItemStack stack = getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (amount >= stack.getCount()) {
            if (!simulate) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
                refill();
            }
            return stack;
        } else {
            if (!simulate) {
                decrStackSize(slot, amount);
                refill();
            }
            ItemStack newStack = stack.copy();
            newStack.setCount(amount);
            return newStack;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 2;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return false;
    }
}
