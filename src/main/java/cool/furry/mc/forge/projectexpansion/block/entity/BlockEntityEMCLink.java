package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

@SuppressWarnings("unused")
public class BlockEntityEMCLink extends BlockEntity implements IEmcStorage, IItemHandler, IHasMatter {
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

    public BlockEntityEMCLink(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.EMC_LINK.get(), pos, state);
        this.itemStack = ItemStack.EMPTY;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner")) this.owner = tag.getUUID("Owner");
        if (tag.contains("OwnerName", Tag.TAG_STRING)) this.ownerName = tag.getString("OwnerName");
        if (tag.contains("EMC", Tag.TAG_STRING)) emc = new BigInteger(tag.getString(("EMC")));
        if (tag.contains("Item", Tag.TAG_COMPOUND)) itemStack = NBTManager.getPersistentInfo(ItemInfo.fromStack(ItemStack.of(tag.getCompound("Item")))).createStack();
        if (tag.contains("RemainingEMC", Tag.TAG_DOUBLE)) remainingEMC = (long) tag.getDouble("RemainingEMC");
        if (tag.contains("RemainingImport", Tag.TAG_INT)) remainingImport = tag.getInt("RemainingImport");
        if (tag.contains("RemainingExport", Tag.TAG_INT)) remainingExport = tag.getInt("RemainingExport");
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("Owner", this.owner);
        tag.putString("OwnerName", this.ownerName);
        tag.putString("EMC", emc.toString());
        tag.put("Item", itemStack.serializeNBT());
        tag.putDouble("RemainingEMC", remainingEMC);
        tag.putInt("RemainingImport", remainingImport);
        tag.putInt("RemainingExport", remainingExport);
    }

    /********
     * MISC *
     ********/

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityEMCLink blockEntity) {
        // we can't use the user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
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
        remainingEMC = getMatter().getEMCLimit();
        remainingImport = remainingExport = getMatter().getItemLimit();
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = stack.copy();
        itemStack.setCount(1);
        Util.markDirty(this);
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof Player player) setOwner(player);
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
        if (slot != 0 || itemStack.isEmpty()) return ItemStack.EMPTY;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack))).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int count = maxCount.intValueExact();
        if (count <= 0) return ItemStack.EMPTY;

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

        if (count <= 0) return stack;

        int insertCount = Math.min(count, remainingImport);
        if (!simulate) {
            long itemValue = ProjectEAPI.getEMCProxy().getSellValue(stack);
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
            BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
            provider.setEmc(provider.getEmc().add(totalValue));
            ServerPlayer player = Util.getPlayer(owner);
            if (player != null) {
                if (provider.addKnowledge(stack))
                    provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromStack(stack)), true);
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
        if (slot != 0 || remainingExport <= 0 || owner == null || itemStack.isEmpty()) return ItemStack.EMPTY;

        BigInteger itemValue = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack));
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
        int extractCount = Math.min(amount, limit ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
        if (extractCount <= 0) return ItemStack.EMPTY;

        ItemStack r = itemStack.copy();
        r.setCount(extractCount);
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
        return getMatter().getItemLimit();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return ProjectEAPI.getEMCProxy().hasValue(stack);
    }

    public InteractionResult handleActivation(Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        if(!owner.equals(player.getUUID())) {
            player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.not_owner", new TextComponent(ownerName).setStyle(ColorStyle.RED)).setStyle(ColorStyle.RED), true);
            return InteractionResult.CONSUME;
        }
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
                player.displayClientMessage(new TranslatableComponent("block.projectexpansion.emc_link.not_enough_emc", new TextComponent(String.valueOf(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(itemStack)))).setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.RED), true);
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
                (cap == PECapabilities.EMC_STORAGE_CAPABILITY) ? this.emcStorageCapability.cast() :
                        (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? this.itemHandlerCapability.cast() :
                                super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        this.emcStorageCapability.invalidate();
        this.itemHandlerCapability.invalidate();
    }
}
