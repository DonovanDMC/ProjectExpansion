package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class TileEMCLink extends TileEntity implements ITickableTileEntity, IEmcStorage, IItemHandler, IHasMatter {
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    public BigInteger emc = BigInteger.ZERO;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    private ItemStack itemStack;
    private Matter matter;
    private long remainingEMC = 0L;
    private int remainingImport = 0;
    private int remainingExport = 0;

    public TileEMCLink() {
        super(TileEntityTypes.EMC_LINK.get());
        this.itemStack = ItemStack.EMPTY;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId("Owner")) this.owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING)) this.ownerName = nbt.getString("OwnerName");
        if (nbt.contains("EMC", Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString(("EMC")));
        if (nbt.contains("Item", Constants.NBT.TAG_COMPOUND)) itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.read(nbt.getCompound("Item")))).createStack();
        if (nbt.contains("RemainingEMC", Constants.NBT.TAG_DOUBLE)) remainingEMC = (long) nbt.getDouble("RemainingEMC");
        if (nbt.contains("RemainingImport", Constants.NBT.TAG_INT)) remainingImport = nbt.getInt("RemainingImport");
        if (nbt.contains("RemainingExport", Constants.NBT.TAG_INT)) remainingExport = nbt.getInt("RemainingExport");
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", this.owner);
        nbt.putString("OwnerName", this.ownerName);
        nbt.putString("EMC", emc.toString());
        nbt.put("Item", itemStack.serializeNBT());
        nbt.putDouble("RemainingEMC", remainingEMC);
        nbt.putInt("RemainingImport", remainingImport);
        nbt.putInt("RemainingExport", remainingExport);
        return nbt;
    }

    /********
     * MISC *
     ********/

    @Override
    public void tick() {
        // we can't use the user defined value due to emc duplication possibilities
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
        remainingEMC = getMatter().getEMCLimit();
        remainingImport = remainingExport = getMatter().getItemLimit();
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUniqueID();
        this.ownerName = player.getScoreboardName();
        markDirty();
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = stack;
        markDirty();
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
        resetLimits();
    }

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

    public Direction getDirection() {
        return getBlockState().get(BlockStateProperties.HORIZONTAL_FACING);
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
        return getMatter().getEMCLimit();
    }

    @Override
    public long extractEmc(long emc, EmcAction action) {
        return emc < 0L ? insertEmc(-emc, action) : 0L;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        long v = Math.min(remainingEMC, emc);

        if (emc <= 0L)
            return 0L;

        if (action.execute())
            this.emc = this.emc.add(BigInteger.valueOf(v));

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
        if (slot != 0 || itemStack.isEmpty())
            return ItemStack.EMPTY;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack))).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int count = maxCount.intValueExact();
        if (count <= 0)
            return ItemStack.EMPTY;

        ItemStack stack = itemStack.copy();
        stack.setCount(count);
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot == 0 || remainingImport <= 0 || owner == null || stack.isEmpty() || !isItemValid(slot, stack))
            return stack;

        stack = stack.copy();
        int count = stack.getCount();
        stack.setCount(1);

        if (count <= 0)
            return stack;

        int insertCount = Math.min(count, remainingImport);
        if (!simulate) {
            long itemValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
            BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
            provider.setEmc(provider.getEmc().add(totalValue));
            ServerPlayerEntity player = Util.getPlayer(owner);
            if (player != null) {
                if (provider.addKnowledge(stack)) {
                    provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromStack(stack)), true);
                }
                provider.syncEmc(player);
            }
            remainingImport -= insertCount;
            markDirty();
        }

        if (insertCount == count) {
            return ItemStack.EMPTY;
        }

        stack.setCount(count - insertCount);
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItemInternal(slot, amount, simulate, true);
    }

    public ItemStack extractItemInternal(int slot, int amount, boolean simulate, boolean limit) {
        if (slot != 0 || remainingExport <= 0 || owner == null || itemStack.isEmpty())
            return ItemStack.EMPTY;

        BigInteger itemValue = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int extractCount = Math.min(amount, limit ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
        if (extractCount <= 0)
            return ItemStack.EMPTY;

        ItemStack r = itemStack.copy();
        r.setCount(extractCount);
        if (simulate) return r;

        BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
        provider.setEmc(provider.getEmc().subtract(totalPrice));
        ServerPlayerEntity player = Util.getPlayer(owner);
        if (player != null)
            provider.syncEmc(player);

        if (limit) remainingExport -= extractCount;
        markDirty();
        return r;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getMatter().getItemLimit();
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
            (cap == ProjectEAPI.EMC_STORAGE_CAPABILITY) ? this.emcStorageCapability.cast() :
            (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? this.itemHandlerCapability.cast() :
                super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        this.itemHandlerCapability.invalidate();
    }

    public ActionResultType handleActivation(PlayerEntity player, Hand hand) {
        ItemStack inHand = player.getHeldItem(hand);
        if(!owner.equals(player.getUniqueID())) {
            player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_owner", new StringTextComponent(ownerName).mergeStyle(TextFormatting.RED)).mergeStyle(TextFormatting.RED), true);
            return ActionResultType.CONSUME;
        }
        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.cleared").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            if (!isItemValid(0, inHand)) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.no_emc_value", new TranslationTextComponent(itemStack.getTranslationKey()).mergeStyle(TextFormatting.BLUE)).mergeStyle(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            setInternalItem(inHand);
            player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.set", new TranslationTextComponent(itemStack.getTranslationKey()).mergeStyle(TextFormatting.BLUE)).mergeStyle(TextFormatting.GREEN), true);
            return ActionResultType.SUCCESS;
        }

        if (inHand.isEmpty() || itemStack.isItemEqual(inHand)) {
            if (Config.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.no_export_remaining").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemStack extract = extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_enough_emc", new StringTextComponent(String.valueOf(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).mergeStyle(TextFormatting.GREEN)).mergeStyle(TextFormatting.RED), true);
                return ActionResultType.CONSUME;
            }
            ItemHandlerHelper.giveItemToPlayer(player, extract);
            return ActionResultType.SUCCESS;
        }

        player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.empty_hand").mergeStyle(TextFormatting.RED), true);
        return ActionResultType.CONSUME;
    }
}
