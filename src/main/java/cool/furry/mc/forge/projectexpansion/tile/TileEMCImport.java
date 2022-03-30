package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.container.ContainerEMCImport;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.TileEntityInventoryHelper;
import io.netty.buffer.Unpooled;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class TileEMCImport extends TileEntityInventoryHelper implements ITickableTileEntity, INamedContainerProvider, IItemHandler {
    private static final int[] SLOTS = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    private LazyOptional<IItemHandler> itemHandlerCapability;
    private @Nullable
    IEMCProxy proxy = null;
    private boolean isProcessing = false;

    public TileEMCImport() {
        super(TileEntityTypes.EMC_IMPORT.get(), 45);
    }

    @Nullable
    private ServerPlayerEntity getOwnerPlayer() {
        World world = getWorld();
        if (world == null || world.isRemote) return null;
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
        if (proxy == null) proxy = ProjectEAPI.getEMCProxy();
        return proxy;
    }

    private void process() {
        if (isProcessing) return;

        if (getWorld() == null || getWorld().isRemote) return;
        isProcessing = true;

        IKnowledgeProvider provider = getProvider();
        BigInteger gain = BigInteger.ZERO;
        ServerPlayerEntity player = getOwnerPlayer();

        int index = 0;
        for (ItemStack stack : getItems()) {
            if (stack.isEmpty() || !getEMC().hasValue(stack.getItem())) {
                index++;
                continue;
            }
            Main.Logger.printf(Level.INFO, "slot:%s contents:%s", index, stack);
            gain = gain.add(BigInteger.valueOf(getEMC().getValue(stack.getItem()) * stack.getCount()));
            setInventorySlotContents(index, ItemStack.EMPTY);
            index++;
        }

        // Main.Logger.printf(Level.INFO, "%s %s %s", provider.getEmc(), gain, player == null);

        if (gain.equals(BigInteger.ZERO)) {
            isProcessing = false;
            return;
        }

        // the emc isn't changing????
        Main.Logger.printf(Level.INFO, "%s %s %s %s %s", provider.getEmc(), gain, provider.getEmc().add(gain), owner, player);
        provider.setEmc(provider.getEmc().add(gain));
        Main.Logger.info(provider.getEmc());
        if (player != null) provider.syncEmc(player);
        isProcessing = false;
    }

    @Override
    public void tick() {
        process();
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getEMC().hasValue(stack.getItem());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
        return getEMC().hasValue(stack.getItem());
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ContainerEMCImport(windowId, playerInventory, new PacketBuffer(Unpooled.buffer()).writeBlockPos(pos));
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("block.projectexpansion.emc_import");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (itemHandlerCapability == null || !itemHandlerCapability.isPresent()) {
                itemHandlerCapability = LazyOptional.of(() -> this);
            }

            return itemHandlerCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        ItemStack oldStack = getStackInSlot(slot);
        if (oldStack.isEmpty()) {
            if (!simulate) {
                setInventorySlotContents(slot, stack);
                process();
            }
            return ItemStack.EMPTY;
        } else {
            if (!oldStack.getItem().equals(stack.getItem())) return stack;
            int canAdd = oldStack.getCount() - stack.getMaxStackSize();
            int toAdd = stack.getCount();
            if (toAdd > canAdd) {
                toAdd = canAdd;
                oldStack.setCount(oldStack.getCount() + toAdd);
            }
            int leftover = stack.getCount() - toAdd;
            if (!simulate) {
                setInventorySlotContents(slot, oldStack);
                process();
            }
            if (leftover <= 0) return ItemStack.EMPTY;
            stack.setCount(leftover);
            return stack;
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 45;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return getEMC().hasValue(stack.getItem());
    }
}

