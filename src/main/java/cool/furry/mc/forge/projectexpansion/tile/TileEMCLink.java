package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

@SuppressWarnings("unused")
public class TileEMCLink extends TileNBTFilterable implements ITickableTileEntity, IEmcStorage, IItemHandler, IHasMatter, IFluidHandler {
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IFluidHandler> fluidHandlerCapability = LazyOptional.of(() -> this);
    public BigInteger emc = BigInteger.ZERO;
    private ItemStack itemStack;
    private Matter matter;
    private BigInteger remainingEMC = BigInteger.ZERO;
    private int remainingImport = 0;
    private int remainingExport = 0;
    private int remainingFluid = 0;

    public TileEMCLink() {
        super(TileEntityTypes.EMC_LINK.get());
        itemStack = ItemStack.EMPTY;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.contains(NBTNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString((NBTNames.STORED_EMC)));
        if (nbt.contains(NBTNames.ITEM, Constants.NBT.TAG_COMPOUND)) itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.read(nbt.getCompound(NBTNames.ITEM)))).createStack();
        if (nbt.contains(NBTNames.REMAINING_EMC, Constants.NBT.TAG_STRING)) remainingEMC = new BigInteger(nbt.getString(NBTNames.REMAINING_EMC));
        if (nbt.contains(NBTNames.REMAINING_IMPORT, Constants.NBT.TAG_INT)) remainingImport = nbt.getInt(NBTNames.REMAINING_IMPORT);
        if (nbt.contains(NBTNames.REMAINING_EXPORT, Constants.NBT.TAG_INT)) remainingExport = nbt.getInt(NBTNames.REMAINING_EXPORT);
        if (nbt.contains(NBTNames.REMAINING_FLUID, Constants.NBT.TAG_INT)) remainingFluid = nbt.getInt(NBTNames.REMAINING_FLUID);
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putString(NBTNames.STORED_EMC, emc.toString());
        nbt.put(NBTNames.ITEM, itemStack.serializeNBT());
        nbt.putString(NBTNames.REMAINING_EMC, remainingEMC.toString());
        nbt.putInt(NBTNames.REMAINING_IMPORT, remainingImport);
        nbt.putInt(NBTNames.REMAINING_EXPORT, remainingExport);
        nbt.putInt(NBTNames.REMAINING_FLUID, remainingFluid);
        return nbt;
    }

    /********
     * MISC *
     ********/

    @Override
    public void tick() {
        // due to the nature of per second this block follows, using the config value isn't really possible
        if (world == null || world.isRemote || (world.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        resetLimits();
        if (emc.equals(BigInteger.ZERO)) return;
        ServerPlayerEntity player = Util.getPlayer(world, owner);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);

        provider.setEmc(provider.getEmc().add(emc));
        if (player != null) provider.syncEmc(player);
        markDirty();
        emc = BigInteger.ZERO;
    }

    private void resetLimits() {
        Matter m = getMatter();
        remainingEMC    = m.getEMCLinkEMCLimit();
        remainingImport = remainingExport = m.getEMCLinkItemLimit();
        remainingFluid  = m.getEMCLinkFluidLimit();
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = ItemHandlerHelper.copyStackWithSize(stack, 1);
        markDirty();
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
        resetLimits();
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        if (world != null) {
            BlockEMCLink block = (BlockEMCLink) getBlockState().getBlock();
            if (block.getMatter() != matter) setMatter(block.getMatter());
            return matter;
        }
        return Matter.BASIC;
    }

    private void setMatter(Matter matter) {
        this.matter = matter;
    }

    /*******
     * EMC *
     *******/

    @Override
    public long getStoredEmc() {
        return 0L;
    }

    @Override
    public long getMaximumEmc() {
        return Util.safeLongValue(getMatter().getEMCLinkEMCLimit());
    }

    @Override
    public long extractEmc(long emc, EmcAction action) {
        return emc < 0L ? insertEmc(-emc, action) : 0L;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        long v = Math.min(Util.safeLongValue(remainingEMC), emc);

        if (emc <= 0L) return 0L;
        if (action.execute()) this.emc = this.emc.add(BigInteger.valueOf(v));

        return v;
    }

    /*********
     * Items *
     *********/

    @Override
    public int getSlots() {
        return getMatter().getEMCLinkInventorySize();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0 || itemStack.isEmpty()) return ItemStack.EMPTY;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack))).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int count = maxCount.intValueExact();
        if (count <= 0) return ItemStack.EMPTY;

        return ItemHandlerHelper.copyStackWithSize(itemStack, 1);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot == 0 || remainingImport <= 0 || owner == null || stack.isEmpty() || !isItemValid(slot, stack) || Util.getPlayer(owner) == null) return stack;

        int count = stack.getCount();
        stack = ItemHandlerHelper.copyStackWithSize(stack, 1);

        if (count <= 0) return stack;

        ItemInfo info = ItemInfo.fromStack(stack);
        if(getFilterStatus() && !NBTManager.getPersistentInfo(info).equals(info)) return stack;

        int insertCount = Math.min(count, remainingImport);
        if (!simulate) {
            long itemValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
            BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
            provider.setEmc(provider.getEmc().add(totalValue));
            ServerPlayerEntity player = Util.getPlayer(owner);
            if (player != null) {
                if (provider.addKnowledge(stack)) provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
                provider.syncEmc(player);
            }
            remainingImport -= insertCount;
            markDirty();
        }

        if (insertCount == count) return ItemStack.EMPTY;

        stack.setCount(count - insertCount);
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItemInternal(slot, amount, simulate, true);
    }

    public ItemStack extractItemInternal(int slot, int amount, boolean simulate, boolean limit) {
        if (slot != 0 || remainingExport <= 0 || owner == null || itemStack.isEmpty() || Util.getPlayer(owner) == null) return ItemStack.EMPTY;

        BigInteger itemValue = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int extractCount = Math.min(amount, limit ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
        if (extractCount <= 0) return ItemStack.EMPTY;

        ItemStack r = ItemHandlerHelper.copyStackWithSize(itemStack, extractCount);
        if (simulate) return r;

        BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
        provider.setEmc(provider.getEmc().subtract(totalPrice));
        ServerPlayerEntity player = Util.getPlayer(owner);
        if (player != null) provider.syncEmc(player);

        if (limit) remainingExport -= extractCount;
        markDirty();
        return r;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getMatter().getEMCLinkItemLimit();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return ProjectEAPI.getEMCProxy().hasValue(stack);
    }

    /*********
     * Fluids *
     *********/

    public @Nullable Fluid getFluid() {
        if(!itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem) {
            BucketItem bucketItem = (BucketItem) itemStack.getItem();
            return bucketItem.getFluid();
        } else return null;
    }

    private double getFluidCostPer() {
        try {
            return ProjectEAPI.getEMCProxy().getValue(itemStack) / 1000D;
        } catch(ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    private long getFluidCost(double amount) {
        try {
            double cost = getFluidCostPer();
            return (long) Math.ceil(cost * amount);
        } catch(ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        Fluid fluid = getFluid();
        if(fluid == null) return FluidStack.EMPTY;
        return new FluidStack(fluid, remainingFluid);
    }

    @Override
    public int getTankCapacity(int tank) {
        return remainingFluid;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = getFluid();
        if(fluid != null && resource.getFluid().equals(fluid)) return drain(resource.getAmount(), action);
        return FluidStack.EMPTY;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Fluid fluid = getFluid();
        if(fluid == null  || Util.getPlayer(owner) == null) return FluidStack.EMPTY;
        if(maxDrain > remainingFluid) maxDrain = remainingFluid;
        long cost = getFluidCost(maxDrain);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger emc = provider.getEmc();
        BigDecimal dEMC = new BigDecimal(emc);
        if(dEMC.compareTo(BigDecimal.valueOf(getFluidCostPer())) < 0) return FluidStack.EMPTY;
        if(emc.compareTo(BigInteger.valueOf(cost)) < 0) {
            // this is a bad way to estimate, it rounds up so we'll usually say less than what's really possible
            BigDecimal max = dEMC.divide(BigDecimal.valueOf(getFluidCostPer()), RoundingMode.FLOOR);
            maxDrain = Util.safeIntValue(max);
            if(maxDrain > remainingFluid) maxDrain = remainingFluid;
            if(maxDrain < 1) return FluidStack.EMPTY;
            cost = getFluidCost(maxDrain);
        }
        if(action.execute()) {
            remainingFluid -= maxDrain;
            markDirty();
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            provider.syncEmc(Objects.requireNonNull(Util.getPlayer(owner)));
        }
        return new FluidStack(fluid, maxDrain);
    }

    public ActionResultType handleActivation(PlayerEntity player, Hand hand) {
        ItemStack inHand = player.getHeldItem(hand);

        if(!super.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return ActionResultType.CONSUME;

        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.cleared").setStyle(ColorStyle.RED), true);
                return ActionResultType.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            if (!isItemValid(0, inHand)) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.no_emc_value", new TranslationTextComponent(itemStack.getTranslationKey()).setStyle(ColorStyle.BLUE)).setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            setInternalItem(inHand);
            player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.set", new TranslationTextComponent(itemStack.getTranslationKey()).setStyle(ColorStyle.BLUE)).setStyle(ColorStyle.GREEN), true);
            return ActionResultType.SUCCESS;
        }

        Fluid fluid = getFluid();
        if(fluid != null && inHand.getItem() instanceof BucketItem && ((BucketItem) inHand.getItem()).getFluid() == Fluids.EMPTY) {
            BucketItem bucketItem = (BucketItem) inHand.getItem();
            if(Config.limitEmcLinkVendor.get() && remainingExport < 1000) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.no_export_remaining").setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            long cost = getFluidCost(1000);
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
            BigInteger emc = provider.getEmc();
            if(emc.compareTo(BigInteger.valueOf(cost)) < 0) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_enough_emc", new StringTextComponent(EMCFormat.format(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            FluidActionResult fillResult = FluidUtil.tryFillContainer(inHand, this, 1000, player, true);
            if(!fillResult.isSuccess()) return ActionResultType.FAIL;
            player.inventory.decrStackSize(player.inventory.currentItem, 1);
            ItemHandlerHelper.giveItemToPlayer(player, fillResult.getResult());
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            remainingFluid -= 1000;
            markDirty();
            if(player instanceof ServerPlayerEntity) provider.syncEmc((ServerPlayerEntity) player);
            return ActionResultType.CONSUME;
        }

        if (inHand.isEmpty() || itemStack.isItemEqual(inHand)) {
            if (Config.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.no_export_remaining").setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemStack extract = extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_enough_emc", new StringTextComponent(String.valueOf(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemHandlerHelper.giveItemToPlayer(player, extract);
            return ActionResultType.SUCCESS;
        }

        player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.empty_hand").setStyle(ColorStyle.RED), true);
        return ActionResultType.CONSUME;
    }

    /****************
     * Capabilities *
     ****************/

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) ? emcStorageCapability.cast() :
                (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
                    (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) ? fluidHandlerCapability.cast() :
                            super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        emcStorageCapability.invalidate();
        itemHandlerCapability.invalidate();
        fluidHandlerCapability.invalidate();
    }
}
