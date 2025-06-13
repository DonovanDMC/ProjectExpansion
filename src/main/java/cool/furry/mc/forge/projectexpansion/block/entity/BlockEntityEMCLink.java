package cool.furry.mc.neoforge.projectexpansion.block.entity;

import cool.furry.mc.neoforge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityEMCLink extends BlockEntityNBTFilterable implements IHasMatter {
    public static final ICapabilityProvider<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IEmcStorage> EMC_STORAGE_PROVIDER = (link, side) -> link.getEMCHandler();
    public static final ICapabilityProvider<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IEmcStorageBigInteger> BIG_EMC_STORAGE_PROVIDER = (link, side) -> link.getEMCHandler();
    public static final ICapabilityProvider<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IItemHandler> ITEM_HANDLER_PROVIDER = (link, side) -> link.getItemHandler();
    public static final ICapabilityProvider<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IFluidHandler> FLUID_HANDLER_PROVIDER = (link, side) -> link.getFluidHandler();
    public BigInteger emc = BigInteger.ZERO;
    public ItemStack itemStack;
    public Matter matter;
    public BigInteger remainingEMC = BigInteger.ZERO;
    public int remainingImport = 0;
    public int remainingExport = 0;
    public int remainingFluid = 0;

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PECapabilities.EMC_STORAGE_CAPABILITY, BlockEntityTypes.EMC_LINK.get(), EMC_STORAGE_PROVIDER);
        event.registerBlockEntity(cool.furry.mc.neoforge.projectexpansion.registries.Capabilities.BIG_EMC_STORAGE_CAPABILITY, BlockEntityTypes.EMC_LINK.get(), BIG_EMC_STORAGE_PROVIDER);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityTypes.EMC_LINK.get(), ITEM_HANDLER_PROVIDER);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntityTypes.EMC_LINK.get(), FLUID_HANDLER_PROVIDER);
    }

    public BlockEntityEMCLink(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.EMC_LINK.get(), pos, state);
        itemStack = ItemStack.EMPTY;
    }

    private EMCHandler getEMCHandler() {
        return new EMCHandler();
    }

    private ItemHandler getItemHandler() {
        return new ItemHandler();
    }

    private FluidHandler getFluidHandler() {
        return new FluidHandler();
    }

    /*******
     * NBT *
     *******/

    @Override
    public void loadAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        if (tag.contains(TagNames.ITEM, Tag.TAG_COMPOUND)) itemStack = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(ItemStack.parseOptional(registries, tag.getCompound(TagNames.ITEM)))).createStack();
        if (tag.contains(TagNames.REMAINING_EMC, Tag.TAG_STRING)) remainingEMC = new BigInteger(tag.getString(TagNames.REMAINING_EMC));
        if (tag.contains(TagNames.REMAINING_IMPORT, Tag.TAG_INT)) remainingImport = tag.getInt(TagNames.REMAINING_IMPORT);
        if (tag.contains(TagNames.REMAINING_EXPORT, Tag.TAG_INT)) remainingExport = tag.getInt(TagNames.REMAINING_EXPORT);
        if (tag.contains(TagNames.REMAINING_FLUID, Tag.TAG_INT)) remainingFluid = tag.getInt(TagNames.REMAINING_FLUID);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.put(TagNames.ITEM, itemStack.saveOptional(registries));
        tag.putString(TagNames.REMAINING_EMC, remainingEMC.toString());
        tag.putInt(TagNames.REMAINING_IMPORT, remainingImport);
        tag.putInt(TagNames.REMAINING_EXPORT, remainingExport);
        tag.putInt(TagNames.REMAINING_FLUID, remainingFluid);
    }

    /********
     * MISC *
     ********/

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityEMCLink be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityEMCLink blockEntity) {
        // due to the nature of per second this block follows, using the config value isn't really possible
        if (level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        resetLimits();
        if (emc.equals(BigInteger.ZERO)) return;
        ServerPlayer player = Util.getServerPlayer(level, owner);
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
        itemStack = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack.copyWithCount(1))).createStack();
        markDirty();
    }

    @Override
    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        super.handlePlace(livingEntity, stack);
        resetLimits();
    }

    @Override
    public @NotNull Matter getMatter() {
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

    public InteractionResult handleActivation(Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        ItemHandler itemHandler = getItemHandlerCapability();
        FluidHandler fluidHandler = getFluidHandlerCapability();

        if(!super.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return InteractionResult.CONSUME;

        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.displayClientMessage(Lang.Blocks.EMC_LINK_CLEARED.translateColored(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (!itemHandler.isItemValid(0, inHand)) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(ChatFormatting.RED, Component.translatable(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
                return InteractionResult.CONSUME;
            }
            setInternalItem(inHand);
            player.displayClientMessage(Lang.Blocks.EMC_LINK_SET.translateColored(ChatFormatting.GREEN, Component.literal(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
            return InteractionResult.SUCCESS;
        }

        Fluid fluid = fluidHandler.getFluid();
        if(fluid != null && fluidHandler.isValid() && inHand.getItem() instanceof BucketItem bucketItem && ((BucketItem) inHand.getItem()).content == Fluids.EMPTY) {
            if(Config.server.limitEmcLinkVendor.get() && remainingFluid < 1000) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            long cost = fluidHandler.getFluidCost(1000);
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if(provider == null) {
                player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, Util.getServerPlayer(owner) == null ? owner : Objects.requireNonNull(Util.getServerPlayer(owner)).getDisplayName()), true);
                return InteractionResult.FAIL;
            }
            BigInteger emc = provider.getEmc();
            if(emc.compareTo(BigInteger.valueOf(cost)) < 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(ChatFormatting.RED, Component.literal(EMCFormat.format(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack)))).setStyle(ColorStyle.GREEN)), true);
                return InteractionResult.CONSUME;
            }
            FluidActionResult fillResult = FluidUtil.tryFillContainer(inHand, fluidHandler, 1000, player, true);
            if(!fillResult.isSuccess()) return InteractionResult.FAIL;
            player.getInventory().removeItem(player.getInventory().selected, 1);
            ItemHandlerHelper.giveItemToPlayer(player, fillResult.getResult());
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            remainingFluid -= 1000;
            markDirty();
            if(player instanceof ServerPlayer) provider.syncEmc((ServerPlayer) player);
            return InteractionResult.CONSUME;
        }

        if (inHand.isEmpty() || itemStack.is(inHand.getItem())) {
            if (Config.server.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            ItemStack extract = itemHandler.extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.server.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(ChatFormatting.RED, Component.literal(EMCFormat.format(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack)))).setStyle(ColorStyle.GREEN)), true);
                return InteractionResult.CONSUME;
            }
            ItemHandlerHelper.giveItemToPlayer(player, extract);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(ChatFormatting.RED), true);
        return InteractionResult.CONSUME;
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

        @Override
        public BigInteger extractEmcBigInteger(BigInteger value, EmcAction action) {
            return emc.compareTo(BigInteger.ZERO) < 0 ? insertEmcBigInteger(value.negate(), action) : value;
        }

        @Override
        public BigInteger insertEmcBigInteger(BigInteger value, EmcAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            BigInteger v = isFinal ? emc : remainingEMC.min(emc);

            if (emc.compareTo(BigInteger.ZERO) < 0) return BigInteger.ZERO;
            if (action.execute()) {
                if(!isFinal) remainingEMC = remainingEMC.subtract(v);
                emc = emc.add(v);
                markDirty();
            }

            return v;
        }
    }

    EMCHandler getEMCHandlerCapability() {
        return (EMCHandler) WorldHelper.getCapability(level, PECapabilities.EMC_STORAGE_CAPABILITY, worldPosition, getBlockState(), this, null);
    }

    private class ItemHandler implements IItemHandler {
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
            BigInteger val = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack));
            if(val.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(val).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int count = maxCount.intValueExact();
            if (count <= 0) return ItemStack.EMPTY;

            return itemStack.copyWithCount(count);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot == 0 || (!isFinal && remainingImport <= 0) || owner == null || stack.isEmpty() || !isItemValid(slot, stack) || Util.getServerPlayer(owner) == null) return stack;

            int count = stack.getCount();
            stack = stack.copyWithCount(1);

            if (count <= 0) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);
            if(getFilterStatus() && !IEMCProxy.INSTANCE.getPersistentInfo(info).equals(info)) return stack;

            int insertCount = isFinal ? count : Math.min(count, remainingImport);
            if (!simulate) {
                long itemValue = IEMCProxy.INSTANCE.getSellValue(stack);
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
                if (provider == null) return stack;
                BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
                provider.setEmc(provider.getEmc().add(totalValue));
                ServerPlayer player = Util.getServerPlayer(owner);
                if (player != null) {
                    if (provider.addKnowledge(stack))
                        provider.syncKnowledgeChange(player, IEMCProxy.INSTANCE.getPersistentInfo(info), true);
                    provider.syncEmc(player);
                }
                if(!isFinal) remainingImport -= insertCount;
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
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot != 0 || (!isFinal && remainingExport <= 0) || owner == null || itemStack.isEmpty() || Util.getServerPlayer(owner) == null) return ItemStack.EMPTY;

            BigInteger itemValue = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack));
            if(itemValue.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int extractCount = Math.min(amount, limit && !isFinal ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
            if (extractCount <= 0) return ItemStack.EMPTY;

            ItemStack r = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack.copy())).createStack();
            r.setCount(extractCount);
            if (simulate) return r;

            BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
            provider.setEmc(provider.getEmc().subtract(totalPrice));
            ServerPlayer player = Util.getServerPlayer(owner);
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
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return IEMCProxy.INSTANCE.hasValue(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return (ItemHandler) WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, worldPosition, getBlockState(), this, null);
    }

    private class FluidHandler implements IFluidHandler {
        public @Nullable Fluid getFluid() {
            if(!itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem bucketItem) return bucketItem.content;
            else return null;
        }

        private double getFluidCostPer() {
            try {
                long fullCost = IEMCProxy.INSTANCE.getValue(itemStack);
                long bucketCost = IEMCProxy.INSTANCE.getValue(net.minecraft.world.item.Items.BUCKET);
                if (bucketCost == 0 && fullCost == 0) return 0D;
                return (fullCost - ((bucketCost * getMatter().getFluidEfficiencyPercentage()) / 100F))  / 1000D;
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

        @Nonnull
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
            if (!isValid() || !resource.getFluid().equals(fluid)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            Fluid fluid = getFluid();
            if(fluid == null || !isValid() || Util.getServerPlayer(owner) == null) return FluidStack.EMPTY;
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
                    provider.syncEmc(Objects.requireNonNull(Util.getServerPlayer(owner)));
                }
            }
            return new FluidStack(fluid, maxDrain);
        }
    }

    FluidHandler getFluidHandlerCapability() {
        return (FluidHandler) WorldHelper.getCapability(level, Capabilities.FluidHandler.BLOCK, worldPosition, getBlockState(), this, null);
    }
}
