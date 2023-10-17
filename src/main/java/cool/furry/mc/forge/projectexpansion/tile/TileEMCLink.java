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
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
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
public class TileEMCLink extends TileNBTFilterable implements ITickableTileEntity, IHasMatter {
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(EMCHandler::new);
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(ItemHandler::new);
    private final LazyOptional<IFluidHandler> fluidHandlerCapability = LazyOptional.of(FluidHandler::new);
    public BigInteger emc = BigInteger.ZERO;
    public ItemStack itemStack;
    public Matter matter;
    public BigInteger remainingEMC = BigInteger.ZERO;
    public int remainingImport = 0;
    public int remainingExport = 0;
    public int remainingFluid = 0;

    public TileEMCLink() {
        super(TileEntityTypes.EMC_LINK.get());
        itemStack = ItemStack.EMPTY;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains(NBTNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString((NBTNames.STORED_EMC)));
        if (nbt.contains(NBTNames.ITEM, Constants.NBT.TAG_COMPOUND)) itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.of(nbt.getCompound(NBTNames.ITEM)))).createStack();
        if (nbt.contains(NBTNames.REMAINING_EMC, Constants.NBT.TAG_STRING)) remainingEMC = new BigInteger(nbt.getString(NBTNames.REMAINING_EMC));
        if (nbt.contains(NBTNames.REMAINING_IMPORT, Constants.NBT.TAG_INT)) remainingImport = nbt.getInt(NBTNames.REMAINING_IMPORT);
        if (nbt.contains(NBTNames.REMAINING_EXPORT, Constants.NBT.TAG_INT)) remainingExport = nbt.getInt(NBTNames.REMAINING_EXPORT);
        if (nbt.contains(NBTNames.REMAINING_FLUID, Constants.NBT.TAG_INT)) remainingFluid = nbt.getInt(NBTNames.REMAINING_FLUID);
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        super.save(nbt);
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
        if (level == null || level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        resetLimits();
        if (emc.equals(BigInteger.ZERO)) return;
        ServerPlayerEntity player = Util.getPlayer(level, owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return;

        BigInteger toAdd = getMatter() == Matter.FINAL ? emc : remainingEMC.min(emc);
        provider.setEmc(provider.getEmc().add(toAdd));
        emc = emc.subtract(toAdd).max(BigInteger.ZERO);
        if (player != null) provider.syncEmc(player);
        Util.markDirty(this);
    }

    private void resetLimits() {
        Matter m = getMatter();
        remainingEMC    = m.getEMCLinkEMCLimit();
        remainingImport = remainingExport = m.getEMCLinkItemLimit();
        remainingFluid  = m.getEMCLinkFluidLimit();
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemHandlerHelper.copyStackWithSize(stack, 1))).createStack();
        Util.markDirty(this);
    }

    @Override
    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        super.handlePlace(livingEntity, stack);
        resetLimits();
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        if (level != null) {
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
     * Capabilities *
     *******/

    class EMCHandler implements IEmcStorage {
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
            boolean isFinal = getMatter() == Matter.FINAL;
            long v = isFinal ? emc : Math.min(Util.safeLongValue(remainingEMC), emc);

            if (emc <= 0L) return 0L;
            if (action.execute()) {
                if (!isFinal) remainingEMC = remainingEMC.subtract(BigInteger.valueOf(v));
                TileEMCLink.this.emc = TileEMCLink.this.emc.add(BigInteger.valueOf(v));
                Util.markDirty(TileEMCLink.this);
            }

            return v;
        }
    }

    EMCHandler getEMCHandlerCapability() {
        return (EMCHandler) getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY).orElseThrow(NullPointerException::new);
    }

    class ItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return getMatter().getEMCLinkInventorySize();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0 || itemStack.isEmpty()) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger val = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
            if (val.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(val).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int count = maxCount.intValueExact();
            if (count <= 0) return ItemStack.EMPTY;

            return ItemHandlerHelper.copyStackWithSize(itemStack, count);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot == 0 || (!isFinal && remainingImport <= 0) || owner == null || stack.isEmpty() || !isItemValid(slot, stack) || Util.getPlayer(owner) == null)
                return stack;

            int count = stack.getCount();
            stack = ItemHandlerHelper.copyStackWithSize(stack, 1);

            if (count <= 0) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);
            if (getFilterStatus() && !NBTManager.getPersistentInfo(info).equals(info)) return stack;

            int insertCount = isFinal ? count : Math.min(count, remainingImport);
            if (!simulate) {
                long itemValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
                if (provider == null) return stack;
                BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
                provider.setEmc(provider.getEmc().add(totalValue));
                ServerPlayerEntity player = Util.getPlayer(owner);
                if (player != null) {
                    if (provider.addKnowledge(stack))
                        provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
                    provider.syncEmc(player);
                }
                if (!isFinal) remainingImport -= insertCount;
                Util.markDirty(TileEMCLink.this);
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
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot != 0 || (!isFinal && remainingExport <= 0) || owner == null || itemStack.isEmpty() || Util.getPlayer(owner) == null)
                return ItemStack.EMPTY;

            BigInteger itemValue = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
            if (itemValue.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int extractCount = Math.min(amount, limit && !isFinal ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
            if (extractCount <= 0) return ItemStack.EMPTY;

            ItemStack r = NBTManager.getPersistentInfo(ItemInfo.fromStack(itemStack.copy())).createStack();
            r.setCount(extractCount);
            if (simulate) return r;

            BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
            provider.setEmc(provider.getEmc().subtract(totalPrice));
            ServerPlayerEntity player = Util.getPlayer(owner);
            if (player != null) provider.syncEmc(player);

            if (limit && !isFinal) remainingExport -= extractCount;
            Util.markDirty(TileEMCLink.this);
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
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(NullPointerException::new);
    }

    class FluidHandler implements IFluidHandler {
        public @Nullable Fluid getFluid() {
            if (!itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem) {
                BucketItem bucketItem = (BucketItem) itemStack.getItem();
                return bucketItem.getFluid();
            } else return null;
        }

        private double getFluidCostPer() {
            try {
                long fullCost = ProjectEAPI.getEMCProxy().getValue(itemStack);
                long bucketCost = ProjectEAPI.getEMCProxy().getValue(Items.BUCKET);
                if (bucketCost == 0 && fullCost == 0) return 0D;
                return (fullCost - ((bucketCost * getMatter().getFluidEfficiencyPercentage()) / 100F)) / 1000D;
            } catch (ArithmeticException ignore) {
                return Long.MAX_VALUE;
            }
        }

        private boolean isFreeFluid() {
            return getFluidCostPer() == 0D && Config.zeroEmcFluidsAreFree.get();
        }

        private boolean isValid() {
            return getFluid() != null && (getFluidCostPer() != 0D || isFreeFluid());
        }

        private long getFluidCost(double amount) {
            try {
                double cost = getFluidCostPer();
                return (long) Math.ceil(cost * amount);
            } catch (ArithmeticException ignore) {
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
            if(tank != 0) {
                return FluidStack.EMPTY;
            }

            Fluid fluid = getFluid();
            if (fluid == null || !isValid()) return FluidStack.EMPTY;
            return new FluidStack(fluid, remainingFluid);
        }

        @Override
        public int getTankCapacity(int tank) {
            if(tank != 0) {
                return 0;
            }

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
            if (fluid == null || !isValid() || !resource.getFluid().equals(fluid)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            Fluid fluid = getFluid();
            if (fluid == null || !isValid() || Util.getPlayer(owner) == null) return FluidStack.EMPTY;
            if (!isFinal && maxDrain > remainingFluid) maxDrain = remainingFluid;
            long cost = getFluidCost(maxDrain);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return FluidStack.EMPTY;
            BigInteger emc = provider.getEmc();
            BigDecimal dEMC = new BigDecimal(emc);
            if (dEMC.compareTo(BigDecimal.valueOf(getFluidCostPer())) < 0) return FluidStack.EMPTY;
            if (emc.compareTo(BigInteger.valueOf(cost)) < 0) {
                // this is a bad way to estimate, it rounds up so we'll usually say less than what's really possible
                BigDecimal max = dEMC.divide(BigDecimal.valueOf(getFluidCostPer()), RoundingMode.FLOOR);
                maxDrain = Util.safeIntValue(max);
                if (!isFinal && maxDrain > remainingFluid) maxDrain = remainingFluid;
                if (maxDrain < 1) return FluidStack.EMPTY;
                cost = getFluidCost(maxDrain);
            }
            if (action.execute()) {
                if (!isFinal) remainingFluid -= maxDrain;
                Util.markDirty(TileEMCLink.this);
                if(!isFreeFluid()) {
                    provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
                    provider.syncEmc(Objects.requireNonNull(Util.getPlayer(owner)));
                }
            }
            return new FluidStack(fluid, maxDrain);
        }
    }

    FluidHandler getFluidHandlerCapability() {
        return (FluidHandler) getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).orElseThrow(NullPointerException::new);
    }

    public ActionResultType handleActivation(PlayerEntity player, Hand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        ItemHandler itemHandler = getItemHandlerCapability();
        FluidHandler fluidHandler = getFluidHandlerCapability();

        if(!super.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return ActionResultType.CONSUME;

        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.displayClientMessage(Lang.Blocks.EMC_LINK_CLEARED.translateColored(TextFormatting.RED), true);
                return ActionResultType.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            if (!itemHandler.isItemValid(0, inHand)) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(TextFormatting.RED, new StringTextComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
                return ActionResultType.CONSUME;
            }
            setInternalItem(inHand);
            player.displayClientMessage(Lang.Blocks.EMC_LINK_SET.translateColored(TextFormatting.GREEN, new StringTextComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
            return ActionResultType.SUCCESS;
        }

        Fluid fluid = fluidHandler.getFluid();
        if(fluid != null && fluidHandler.isValid() && inHand.getItem() instanceof BucketItem && ((BucketItem) inHand.getItem()).getFluid() == Fluids.EMPTY) {
            BucketItem bucketItem = (BucketItem) inHand.getItem();
            if(Config.limitEmcLinkVendor.get() && remainingFluid < 1000) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            long cost = fluidHandler.getFluidCost(1000);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) {
                player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, Util.getPlayer(owner) == null ? owner : Objects.requireNonNull(Util.getPlayer(owner)).getDisplayName()), true);
                return ActionResultType.FAIL;
            }
            BigInteger emc = provider.getEmc();
            if(emc.compareTo(BigInteger.valueOf(cost)) < 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(TextFormatting.RED, new StringTextComponent(EMCFormat.format(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)), true);
                return ActionResultType.CONSUME;
            }
            FluidActionResult fillResult = FluidUtil.tryFillContainer(inHand, fluidHandler, 1000, player, true);
            if(!fillResult.isSuccess()) return ActionResultType.FAIL;
            player.inventory.removeItem(player.inventory.selected, 1);
            ItemHandlerHelper.giveItemToPlayer(player, fillResult.getResult());
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            remainingFluid -= 1000;
            Util.markDirty(this);
            if(player instanceof ServerPlayerEntity) provider.syncEmc((ServerPlayerEntity) player);
            return ActionResultType.CONSUME;
        }

        if (inHand.isEmpty() || itemStack.equals(inHand)) {
            if (Config.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemStack extract = itemHandler.extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(TextFormatting.RED, new StringTextComponent(EMCFormat.format(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)), true);
                return ActionResultType.CONSUME;
            }
            ItemHandlerHelper.giveItemToPlayer(player, extract);
            return ActionResultType.SUCCESS;
        }

        player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(TextFormatting.RED), true);
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
