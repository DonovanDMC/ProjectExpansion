package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.*;
import cool.furry.mc.projectexpansion.util.EmcLock;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityEMCLink extends BlockEntityNBTFilterable implements IHasMatter {
    public BigInteger emc = BigInteger.ZERO;
    public ItemStack itemStack;
    public Matter matter;
    public BigInteger remainingEMC = BigInteger.ZERO;
    public int remainingImport = 0;
    public int remainingExport = 0;
    public int remainingFluid = 0;

    public BlockEntityEMCLink() {
        super(BlockEntityTypes.EMC_LINK.get());
        itemStack = ItemStack.EMPTY;
        this.addCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, this::getEMCHandler);
        this.addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, this::getEMCHandler);
        this.addCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this::getItemHandler);
        this.addCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this::getFluidHandler);
    }

    private EMCHandler getEMCHandler(@Nullable Direction side) {
        return new EMCHandler();
    }

    private ItemHandler getItemHandler(@Nullable Direction side) {
        return new ItemHandler();
    }

    private FluidHandler getFluidHandler(@Nullable Direction side) {
        return new FluidHandler();
    }

    /*******
     * NBT *
     *******/

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        if (tag.contains(TagNames.STORED_EMC, Constants.NBT.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        if (tag.contains(TagNames.ITEM, Constants.NBT.TAG_COMPOUND)) itemStack = ProjectEAPI.getEMCProxy().getPersistentInfo(ItemInfo.fromStack(Common.loadItemStackOptional(tag.getCompound(TagNames.ITEM)))).createStack();
        if (tag.contains(TagNames.REMAINING_EMC, Constants.NBT.TAG_STRING)) remainingEMC = new BigInteger(tag.getString(TagNames.REMAINING_EMC));
        if (tag.contains(TagNames.REMAINING_IMPORT, Constants.NBT.TAG_INT)) remainingImport = tag.getInt(TagNames.REMAINING_IMPORT);
        if (tag.contains(TagNames.REMAINING_EXPORT, Constants.NBT.TAG_INT)) remainingExport = tag.getInt(TagNames.REMAINING_EXPORT);
        if (tag.contains(TagNames.REMAINING_FLUID, Constants.NBT.TAG_INT)) remainingFluid = tag.getInt(TagNames.REMAINING_FLUID);
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.put(TagNames.ITEM, Common.saveItemStackOptional(itemStack));
        tag.putString(TagNames.REMAINING_EMC, remainingEMC.toString());
        tag.putInt(TagNames.REMAINING_IMPORT, remainingImport);
        tag.putInt(TagNames.REMAINING_EXPORT, remainingExport);
        tag.putInt(TagNames.REMAINING_FLUID, remainingFluid);
    }

    /********
     * MISC *
     ********/

    public static void tickServer(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
        if (blockEntity instanceof BlockEntityEMCLink) ((BlockEntityEMCLink) blockEntity).tickServer(level, pos, state, ((BlockEntityEMCLink) blockEntity));
    }

    public void tickServer(World level, BlockPos pos, BlockState state, BlockEntityEMCLink blockEntity) {
        // due to the nature of per second this block follows, using the config value isn't really possible
        if (level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        resetLimits();
        if (emc.equals(BigInteger.ZERO)) return;
        ServerPlayerEntity player = Util.getPlayer(level, owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return;

        BigInteger toAdd = getMatter() == Matter.FINAL ? emc : remainingEMC.min(emc);
        provider.setEmc(provider.getEmc().add(toAdd));
        emc = emc.subtract(toAdd).max(BigInteger.ZERO);
        if (player != null) provider.syncEmc(player);
        markDirty(level, pos);
        emc = BigInteger.ZERO;
    }

    private void resetLimits() {
        Matter m = getMatter();
        remainingEMC    = m.getEMCLinkEMCLimit();
        remainingImport = remainingExport = m.getEMCLinkItemLimit();
        remainingFluid  = m.getEMCLinkFluidLimit();
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = ProjectEAPI.getEMCProxy().getPersistentInfo(ItemInfo.fromStack(ItemHandlerHelper.copyStackWithSize(stack, 1))).createStack();
        markDirty();
    }

    @Override
    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        super.handlePlace(livingEntity, stack);
        resetLimits();
    }

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
                player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(TextFormatting.RED, new TranslationTextComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
                return ActionResultType.CONSUME;
            }
            setInternalItem(inHand);
            player.displayClientMessage(Lang.Blocks.EMC_LINK_SET.translateColored(TextFormatting.GREEN, new StringTextComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
            return ActionResultType.SUCCESS;
        }

        Fluid fluid = fluidHandler.getFluid();
        if(fluid != null && fluidHandler.isValid() && inHand.getItem() instanceof BucketItem && ((BucketItem) inHand.getItem()).getFluid() == Fluids.EMPTY) {
            BucketItem bucketItem = (BucketItem) inHand.getItem();
            if(Config.server.limitEmcLinkVendor.get() && remainingFluid < 1000) {
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
            // the empty bucket comes out of the hand that was used, not always the selected hotbar slot
            inHand.shrink(1);
            if (inHand.isEmpty()) player.setItemInHand(hand, fillResult.getResult());
            else ItemHandlerHelper.giveItemToPlayer(player, fillResult.getResult());
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            remainingFluid -= 1000;
            markDirty();
            if(player instanceof ServerPlayerEntity) provider.syncEmc((ServerPlayerEntity) player);
            return ActionResultType.CONSUME;
        }

        if (inHand.isEmpty() || itemStack.getItem() == inHand.getItem()) {
            if (Config.server.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemStack extract = itemHandler.extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.server.limitEmcLinkVendor.get());
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

    private class EMCHandler implements IEmcStorageBigInteger {
        @Override
        public BigInteger getStoredEmcBigInteger() {
            return BigInteger.ZERO;
        }
        @Override
        public BigInteger getMaximumEmcBigInteger() {
            return getMatter().getEMCLinkEMCLimit();
        }

        // emc only goes in, it's handed to the owner straight away so there's never any to take back out
        @Override
        public BigInteger extractEmcBigInteger(BigInteger value, EmcAction action) {
            return value.signum() < 0 ? insertEmcBigInteger(value.negate(), action) : BigInteger.ZERO;
        }

        @Override
        public BigInteger insertEmcBigInteger(BigInteger value, EmcAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            BigInteger v = isFinal ? value : remainingEMC.min(value);

            if (value.compareTo(BigInteger.ZERO) < 0) return BigInteger.ZERO;
            if (action.execute()) {
                if (!isFinal) remainingEMC = remainingEMC.subtract(v);
                emc = emc.add(v);
                markDirty();
            }

            return v;
        }
    }

    EMCHandler getEMCHandlerCapability() {
        return (EMCHandler) Util.getCapability(BlockEntityEMCLink.class, ProjectEAPI.EMC_STORAGE_CAPABILITY, level, worldPosition).orElseThrow(() -> new IllegalStateException("failed to get EMC_STORAGE_CAPABILITY for block entity at " + worldPosition));
    }

    private class ItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return getMatter().getEMCLinkInventorySize();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0 || itemStack.isEmpty()) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger val = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
            if(val.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(val).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int count = maxCount.intValueExact();
            if (count <= 0) return ItemStack.EMPTY;

            return ItemHandlerHelper.copyStackWithSize(itemStack, count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot == 0 || (!isFinal && remainingImport <= 0) || owner == null || stack.isEmpty() || !isItemValid(slot, stack) || Util.getPlayer(owner) == null) return stack;

            int count = stack.getCount();
            // the stack passed in is what's given back, so it's left untouched
            ItemStack single = ItemHandlerHelper.copyStackWithSize(stack, 1);

            if (count <= 0) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);
            if(getFilterStatus() && !ProjectEAPI.getEMCProxy().getPersistentInfo(info).equals(info)) return stack;

            int insertCount = isFinal ? count : Math.min(count, remainingImport);
            if (!simulate) {
                long itemValue = ProjectEAPI.getEMCProxy().getSellValue(single);
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
                if (provider == null) return stack;
                BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
                provider.setEmc(provider.getEmc().add(totalValue));
                ServerPlayerEntity player = Util.getPlayer(owner);
                if (player != null) {
                    if (provider.addKnowledge(single))
                        provider.syncKnowledgeChange(player, ProjectEAPI.getEMCProxy().getPersistentInfo(info), true);
                    provider.syncEmc(player);
                }
                if(!isFinal) remainingImport -= insertCount;
                markDirty();
            }

            if (insertCount == count) return ItemStack.EMPTY;

            return ItemHandlerHelper.copyStackWithSize(stack, count - insertCount);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extractItemInternal(slot, amount, simulate, true);
        }

        public ItemStack extractItemInternal(int slot, int amount, boolean simulate, boolean limit) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot != 0 || (!isFinal && remainingExport <= 0) || owner == null || itemStack.isEmpty() || Util.getPlayer(owner) == null) return ItemStack.EMPTY;

            BigInteger itemValue = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
            if(itemValue.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int extractCount = Math.min(amount, limit && !isFinal ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
            if (extractCount <= 0) return ItemStack.EMPTY;

            ItemStack r = ProjectEAPI.getEMCProxy().getPersistentInfo(ItemInfo.fromStack(itemStack.copy())).createStack();
            r.setCount(extractCount);
            if (simulate) return r;

            BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
            provider.setEmc(provider.getEmc().subtract(totalPrice));
            ServerPlayerEntity player = Util.getPlayer(owner);
            if (player != null) provider.syncEmc(player);

            if (limit && !isFinal) remainingExport -= extractCount;
            markDirty();
            return r;
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMatter().getEMCLinkItemLimit();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ProjectEAPI.getEMCProxy().hasValue(stack) && !EmcLock.isLocked(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) Util.getCapability(BlockEntityEMCLink.class, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, level, worldPosition).orElseThrow(() -> new IllegalStateException("failed to get ITEM_HANDLER for block entity at " + worldPosition));
    }

    private class FluidHandler implements IFluidHandler {
        public @Nullable Fluid getFluid() {
            if(!itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem) return ((BucketItem) itemStack.getItem()).getFluid();
            else return null;
        }

        private double getFluidCostPer() {
            try {
                long fullCost = ProjectEAPI.getEMCProxy().getValue(itemStack);
                long bucketCost = ProjectEAPI.getEMCProxy().getValue(net.minecraft.item.Items.BUCKET);
                if (bucketCost == 0 && fullCost == 0) return 0D;
                // a fluid worth less than its bucket would otherwise pay out emc for being taken
                return Math.max(0D, (fullCost - ((bucketCost * getMatter().getFluidEfficiencyPercentage()) / 100F)) / 1000D);
            } catch(ArithmeticException ignore) {
                return Long.MAX_VALUE;
            }
        }

        private boolean isFreeFluid() {
            return getFluidCostPer() == 0D && Config.server.zeroEmcFluidsAreFree.get();
        }

        private boolean isValid() {
            return getFluid() != null && (getFluidCostPer() != 0D || isFreeFluid());
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

        @Override
        public FluidStack getFluidInTank(int tank) {
            if(tank != 0) {
                return FluidStack.EMPTY;
            }

            Fluid fluid = getFluid();
            if(fluid == null || !isValid()) return FluidStack.EMPTY;
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
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Fluid fluid = getFluid();
            if (!isValid() || !resource.getFluid().equals(fluid)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            Fluid fluid = getFluid();
            if(fluid == null || !isValid() || Util.getPlayer(owner) == null) return FluidStack.EMPTY;
            if(!isFinal && maxDrain > remainingFluid) maxDrain = remainingFluid;
            if(maxDrain > remainingFluid) maxDrain = remainingFluid;
            long cost = getFluidCost(maxDrain);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) return FluidStack.EMPTY;
            BigInteger emc = provider.getEmc();
            BigDecimal dEMC = new BigDecimal(emc);
            if(dEMC.compareTo(BigDecimal.valueOf(getFluidCostPer())) < 0) return FluidStack.EMPTY;
            if(emc.compareTo(BigInteger.valueOf(cost)) < 0) {
                // this is a bad way to estimate, it rounds up so we'll usually say less than what's really possible
                BigDecimal max = dEMC.divide(BigDecimal.valueOf(getFluidCostPer()), RoundingMode.FLOOR);
                maxDrain = Util.safeIntValue(max);
                if(!isFinal &&maxDrain > remainingFluid) maxDrain = remainingFluid;
                if(maxDrain < 1) return FluidStack.EMPTY;
                cost = getFluidCost(maxDrain);
            }
            if(action.execute()) {
                if(!isFinal) remainingFluid -= maxDrain;
                markDirty();
                if(!isFreeFluid()) {
                    provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
                    provider.syncEmc(Objects.requireNonNull(Util.getPlayer(owner)));
                }
            }
            return new FluidStack(fluid, maxDrain);
        }
    }

    FluidHandler getFluidHandlerCapability() {
        return (FluidHandler) Util.getCapability(BlockEntityEMCLink.class, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, level, worldPosition).orElseThrow(() -> new IllegalStateException("failed to get FLUID_HANDLER for block entity at " + worldPosition));
    }

    @Override
    protected void serverTick() {
        tickServer(level, worldPosition, getBlockState(), this);
    }
}
