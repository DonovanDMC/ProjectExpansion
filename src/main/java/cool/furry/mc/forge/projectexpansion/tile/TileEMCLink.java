package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.init.TileEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class TileEMCLink extends TileEntity implements ITickableTileEntity, IEmcStorage, IItemHandler, IHasMatter {
    public UUID owner = Util.DUMMY_UUID;
    public String ownerName = "";
    public BigInteger emc = BigInteger.ZERO;
    public int tick = 0;
    private final LazyOptional<IEmcStorage> emcStorageCapability = LazyOptional.of(() -> this);
    private final LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this);
    private @Nullable Item item = null;
    private final Matter matter;
    private long remainingEMC = 0L;
    private int remainingImport = 0;
    private int remainingExport = 0;

    public TileEMCLink() {
        super(TileEntityTypes.EMC_LINK.get());
        this.matter = Matter.BASIC;
        resetLimits();
    }

    public TileEMCLink(Matter matter) {
        super(TileEntityTypes.EMC_LINK.get());
        this.matter = matter;
        resetLimits();
    }

    /*******
     * NBT *
     *******/

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId("Owner")) this.owner = nbt.getUniqueId("Owner");
        if (nbt.contains("OwnerName", Constants.NBT.TAG_STRING)) this.ownerName = nbt.getString("OwnerName");
        if (nbt.contains("Tick", Constants.NBT.TAG_BYTE)) tick = nbt.getByte("Tick") & 0xFF;
        if (nbt.contains("EMC", Constants.NBT.TAG_STRING)) emc = new BigInteger(nbt.getString(("EMC")));
        if (nbt.contains("Item", Constants.NBT.TAG_STRING)) item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("Item")));
        if(nbt.contains("RemainingEMC", Constants.NBT.TAG_DOUBLE)) remainingEMC = (long) nbt.getDouble("RemainingEMC");
        if(nbt.contains("RemainingImport", Constants.NBT.TAG_INT)) remainingImport = nbt.getInt("RemainingImport");
        if(nbt.contains("RemainingExport", Constants.NBT.TAG_INT)) remainingExport = nbt.getInt("RemainingExport");
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId("Owner", this.owner);
        nbt.putString("OwnerName", this.ownerName);
        nbt.putByte("Tick", (byte) tick);
        nbt.putString("EMC", emc.toString());
        if(item != null) nbt.putString("Item", item.toString());
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
        if (world == null || world.isRemote) return;
        tick++;
        // due to the nature of per second this block follows, using the
        // config value isn't really possible
        if (tick >= 20) {
            tick = 0;

            resetLimits();
            if (emc.equals(BigInteger.ZERO)) return;
            ServerPlayerEntity player = Util.getPlayer(world, owner);
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);

            provider.setEmc(provider.getEmc().add(emc));
            if(player != null) provider.syncEmc(player);
            markDirty();
            emc = BigInteger.ZERO;
        }
    }

    private void resetLimits() {
        if(matter == null) return;
        remainingEMC = matter.getEMCLimit();
        remainingImport = remainingExport = matter.getItemLimit();
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUniqueID();
        this.ownerName = player.getScoreboardName();
        markDirty();
    }

    public void wasPlaced(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
        resetLimits();
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Nullable
    public Item getItem() {
        return item;
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
        return matter.getEMCLimit();
    }

    @Override
    public long extractEmc(long emc, EmcAction action) {
        return emc < 0L ? insertEmc(-emc, action) : 0L;
    }

    @Override
    public long insertEmc(long emc, EmcAction action) {
        if (emc > 0L) {
            long usableEMC = emc;
            if(usableEMC > remainingEMC) {
                usableEMC = remainingEMC;
            }
            if (action.execute()) this.emc = this.emc.add(BigInteger.valueOf(usableEMC));

            return emc - usableEMC;
        }

        return 0L;
    }

    /*********
     * Items *
     *********/

    @Override
    public int getSlots() {
        return matter.getEMCLinkInventorySize();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 && item != null ? new ItemStack(item, remainingExport) : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot == 0 || remainingImport <= 0 || owner == null || !ProjectEAPI.getEMCProxy().hasValue(stack) || stack.isEmpty() || world == null || world.isRemote)
            return stack;
        int count = stack.getCount();
        if(count > remainingImport) count = remainingImport;
        if(!simulate) {
            long value = ProjectEAPI.getEMCProxy().getValue(stack.getItem()) * count;
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
            ServerPlayerEntity player = Util.getPlayer(owner);
            provider.setEmc(provider.getEmc().add(BigInteger.valueOf(value)));
            if(player != null) {
                if(provider.addKnowledge(stack)) provider.sync(player);
                else provider.syncEmc(player);
            }

            remainingImport -= count;
        }

        return count == stack.getCount() ? ItemStack.EMPTY : new ItemStack(stack.getItem(), count);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || remainingExport <= 0 || owner == null || item == null || world == null || world.isRemote)
            return ItemStack.EMPTY;
        long cost = ProjectEAPI.getEMCProxy().getValue(item);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        BigInteger emc = provider.getEmc();
        if (emc.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
        int count = amount;
        long max = emc.divide(BigInteger.valueOf(cost)).longValue();
        if (max < 1) return ItemStack.EMPTY;
        if (count > remainingExport) count = remainingExport;
        if (!simulate) {
            provider.setEmc(emc.subtract(BigInteger.valueOf(cost * count)));
            ServerPlayerEntity player = Util.getPlayer(owner);
            if (player != null) provider.sync(player);
            remainingExport -= count;
        }
        return new ItemStack(item, count);
    }

    @Override
    public int getSlotLimit(int slot) {
        return matter.getItemLimit();
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
        ItemStack stack = player.getHeldItem(hand);
        if(!owner.equals(player.getUniqueID())) {
            player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_owner", new StringTextComponent(ownerName).mergeStyle(TextFormatting.RED)).mergeStyle(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }
        if(player.isCrouching()) {
            // error if no item & crouching
            if (stack.isEmpty()) {
                if (item == null) {
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.FAIL;
                } else {
                    // clear if no item & crouching
                    item = null;
                    markDirty();
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.cleared").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.SUCCESS;
                }
            } else {
                if (item == null) {
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.already_set").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.FAIL;
                } else {
                    // set if no item & non-empty hand (crouching irrelevant)
                    item = stack.getItem();
                    markDirty();
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.set", new TranslationTextComponent(item.getTranslationKey()).mergeStyle(TextFormatting.BLUE)).mergeStyle(TextFormatting.GREEN), true);
                    return ActionResultType.SUCCESS;
                }
            }
        } else {
            if(stack.isEmpty()) {
                // error if no item & empty hand
                if(item == null) {
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_set").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.FAIL;
                } else {
                    // give if item present & empty hand
                    IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
                    long cost = ProjectEAPI.getEMCProxy().getValue(item);
                    BigInteger emc = provider.getEmc();
                    int count = emc.divide(BigInteger.valueOf(cost)).intValue();
                    if(count > item.getMaxStackSize()) count = item.getMaxStackSize();
                    if(count < 1) {
                        player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.not_enough_emc", new StringTextComponent(String.valueOf(cost)).mergeStyle(TextFormatting.GREEN)).mergeStyle(TextFormatting.RED), true);
                        return ActionResultType.FAIL;
                    }
                    provider.setEmc(emc.subtract(BigInteger.valueOf(cost * count)));
                    if(player instanceof ServerPlayerEntity) provider.sync((ServerPlayerEntity) player);

                    player.setItemStackToSlot((hand.equals(Hand.MAIN_HAND) ? EquipmentSlotType.MAINHAND : EquipmentSlotType.OFFHAND), new ItemStack(item, count));
                    return ActionResultType.SUCCESS;
                }
            } else {
                // set if no item & non-empty hand
                if(item == null) {
                    item = stack.getItem();
                    markDirty();
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.set", new TranslationTextComponent(item.getTranslationKey()).mergeStyle(TextFormatting.BLUE)).mergeStyle(TextFormatting.GREEN), true);
                    return ActionResultType.SUCCESS;
                } else {
                    // error if item & non-empty hand
                    player.sendStatusMessage(new TranslationTextComponent("block.projectexpansion.emc_link.empty_hand").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.FAIL;
                }
            }
        }
    }
}
