package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
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
public class BlockEntityEMCLink extends BlockEntityNBTFilterable implements IEmcStorage, IItemHandler, IHasMatter, IFluidHandler {
    public BigInteger emc = BigInteger.ZERO;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IFluidHandler> fluidHandlerCapability = LazyOptional.of(() -> this);
    private ItemStack itemStack;
    private Matter matter;
    private BigInteger remainingEMC = BigInteger.ZERO;
    private int remainingImport = 0;
    private int remainingExport = 0;
    private int remainingFluid = 0;

    public BlockEntityEMCLink(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.EMC_LINK.get(), pos, state);
        itemStack = ItemStack.EMPTY;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        if (tag.contains(TagNames.ITEM, Tag.TAG_COMPOUND)) itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.of(tag.getCompound(TagNames.ITEM)))).createStack();
        if (tag.contains(TagNames.REMAINING_EMC, Tag.TAG_STRING)) remainingEMC = new BigInteger(tag.getString(TagNames.REMAINING_EMC));
        if (tag.contains(TagNames.REMAINING_IMPORT, Tag.TAG_INT)) remainingImport = tag.getInt(TagNames.REMAINING_IMPORT);
        if (tag.contains(TagNames.REMAINING_EXPORT, Tag.TAG_INT)) remainingExport = tag.getInt(TagNames.REMAINING_EXPORT);
        if (tag.contains(TagNames.REMAINING_FLUID, Tag.TAG_INT)) remainingFluid = tag.getInt(TagNames.REMAINING_FLUID);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.put(TagNames.ITEM, itemStack.serializeNBT());
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
        ServerPlayer player = Util.getPlayer(level, owner);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);

        provider.setEmc(provider.getEmc().add(emc));
        if (player != null) provider.syncEmc(player);
        Util.markDirty(this);
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
        Util.markDirty(this);
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof Player player) setOwner(player);
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
            ServerPlayer player = Util.getPlayer(owner);
            if (player != null) {
                if (provider.addKnowledge(stack))
                    provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(info), true);
                provider.syncEmc(player);
            }
            remainingImport -= insertCount;
            Util.markDirty(this);
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
        ServerPlayer player = Util.getPlayer(owner);
        if (player != null) provider.syncEmc(player);

        if (limit) remainingExport -= extractCount;
        Util.markDirty(this);
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
        if(!itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem bucketItem) return bucketItem.getFluid();
        else return null;
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
            Util.markDirty(this);
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost)));
            provider.syncEmc(Objects.requireNonNull(Util.getPlayer(owner)));
        }
        return new FluidStack(fluid, maxDrain);
    }
    public InteractionResult handleActivation(Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);

        if(!super.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return InteractionResult.CONSUME;

        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.not_set").setStyle(ColorStyle.RED), true);
                return InteractionResult.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.cleared").setStyle(ColorStyle.RED), true);
                return InteractionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.not_set").setStyle(ColorStyle.RED), true);
                return InteractionResult.CONSUME;
            }
            if (!isItemValid(0, inHand)) {
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.no_emc_value", new TranslatableComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)).setStyle(ColorStyle.RED), true);
                return InteractionResult.CONSUME;
            }
            setInternalItem(inHand);
            player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.set", new TextComponent(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)).setStyle(ColorStyle.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        if (inHand.isEmpty() || itemStack.is(inHand.getItem())) {
            if (Config.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.no_export_remaining").setStyle(ColorStyle.RED), true);
                return InteractionResult.CONSUME;
            }
            ItemStack extract = extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.not_enough_emc", new TextComponent(EMCFormat.format(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.RED), true);
                return InteractionResult.CONSUME;
            }
            ItemHandlerHelper.giveItemToPlayer(player, extract);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.empty_hand").setStyle(ColorStyle.RED), true);
        return InteractionResult.CONSUME;
    }

    /****************
     * Capabilities *
     ****************/

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return
            (cap == PECapabilities.EMC_STORAGE_CAPABILITY) ? emcStorageCapability.cast() :
                (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? itemHandlerCapability.cast() :
                    (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) ? fluidHandlerCapability.cast() :
                        super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        emcStorageCapability.invalidate();
        itemHandlerCapability.invalidate();
        fluidHandlerCapability.invalidate();
    }
}
