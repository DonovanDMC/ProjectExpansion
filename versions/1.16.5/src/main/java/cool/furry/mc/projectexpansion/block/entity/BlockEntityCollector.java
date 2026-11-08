package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.block.BlockCollector;
import cool.furry.mc.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.container.ContainerCollector;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.capability.managing.BasicCapabilityResolver;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import moze_intel.projecte.capability.managing.SidedItemHandlerResolver;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.tiles.RelayMK1Tile;
import moze_intel.projecte.gameObjs.tiles.WrappedItemHandler;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class BlockEntityCollector extends BlockEntityEMC implements IHasMatter, IHasSunBonus, IGeneratesEMC, INamedContainerProvider {
    private final ItemStackHandler input = new StackHandler(getInvSize()) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            needsCompacting = true;
        }
    };
    private final StackHandler auxSlots = new StackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (slot == UPGRADING_SLOT) {
                needsCompacting = true;
            }
        }
    };
    private final CombinedInvWrapper toSort = new CombinedInvWrapper(new RangedWrapper(auxSlots, UPGRADING_SLOT, UPGRADING_SLOT + 1), input);
    public static final int UPGRADING_SLOT = 0;
    public static final int UPGRADE_SLOT = 1;
    public static final int LOCK_SLOT = 2;

    private final IItemHandlerModifiable automationAuxSlots;
    private final IItemHandlerModifiable automationInput;
    private final IItemHandler joined;


    public boolean hasChargeableItem;
    public boolean hasFuel;
    private BigDecimal unprocessedEMC = BigDecimal.ZERO;
    //Start as needing to check for compacting when loaded
    private boolean needsCompacting = true;
    private Matter matter;
    public BlockEntityCollector() {
        super(BlockEntityTypes.COLLECTOR.get());
        this.automationInput = new WrappedItemHandler(input, WrappedItemHandler.WriteMode.IN) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return SlotPredicates.COLLECTOR_INV.test(stack) ? super.insertItem(slot, stack, simulate) : stack;
            }
        };
        this.automationAuxSlots = new WrappedItemHandler(auxSlots, WrappedItemHandler.WriteMode.OUT) {
            @Override
            public ItemStack extractItem(int slot, int count, boolean simulate) {
                return slot == UPGRADE_SLOT ? super.extractItem(slot, count, simulate) : ItemStack.EMPTY;
            }
        };
        this.joined = new CombinedInvWrapper(automationInput, automationAuxSlots);
        this.addPECapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, new CollectorItemHandlerProvider());
    }

    // block entities aren't given their state when created before 1.17, so the matter has to be passed in
    public BlockEntityCollector(Matter matter) {
        this();
        this.matter = matter;
        resetStackHandlers();
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        if (state.getBlock() instanceof BlockCollector) {
            this.matter = ((BlockCollector) state.getBlock()).getMatter();
            resetStackHandlers();
        }
        super.load(state, tag);
    }

    private void resetStackHandlers() {
        input.setSize(getInvSize());
        auxSlots.setSize(3);
    }

    @Override
    protected boolean canAcceptEmc() {
        //Collector accepts EMC from providers if it has fuel/chargeable. Otherwise, it sends it to providers
        return hasFuel || hasChargeableItem;
    }

    public IItemHandler getInput() {
        return input;
    }

    public IItemHandler getAux() {
        return auxSlots;
    }

    protected int getInvSize() {
        return Math.min(16, (getMatter().ordinal() + 1) * 4) + 4;
    }

    private ItemStack getUpgraded() {
        return auxSlots.getStackInSlot(UPGRADE_SLOT);
    }

    private ItemStack getLock() {
        return auxSlots.getStackInSlot(LOCK_SLOT);
    }

    private ItemStack getUpgrading() {
        return auxSlots.getStackInSlot(UPGRADING_SLOT);
    }

    public void clearLocked() {
        auxSlots.setStackInSlot(LOCK_SLOT, ItemStack.EMPTY);
    }

    @Override
    protected boolean emcAffectsComparators() {
        return true;
    }

    public static void tickServer(World level, BlockPos pos, BlockState state, TileEntity blockEntity) {
        if (blockEntity instanceof BlockEntityCollector) ((BlockEntityCollector) blockEntity).tickServer(level, pos, state, ((BlockEntityCollector) blockEntity));
    }

    public void tickServer(World level, BlockPos pos, BlockState state, BlockEntityCollector blockEntity) {
        if (Config.server.enableCollectorOptimizations.get() && (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        if (needsCompacting) {
            ItemHelper.compactInventory(toSort);
            needsCompacting = false;
		}
        checkFuelOrKlein();
        updateEmc(level, pos);
        rotateUpgraded();
        updateComparators(level, pos);
    }

    private void updateEmc(World level, BlockPos pos) {
        int ticks = Config.server.enableCollectorOptimizations.get() ? 20 : 1;
        BigDecimal gen = getMatter().getCollectorOutputForTicks(ticks);
        // like projecte's collectors, a second's output can be moved out each tick
        // the lower tiers make less than one a tick, which used to round down to never moving any
        BigInteger transfer = getMatter().getCollectorOutput().multiply(BigInteger.valueOf(ticks));
        if(hasSunBonus() && getSunBonus() != null) {
            gen = gen.multiply(BigDecimal.valueOf(getSunBonus()));
            transfer = transfer.multiply(BigInteger.valueOf(getSunBonus()));
        }
        final BigDecimal generated = gen; // Thanks Java
        if (!this.hasMaxedEmc()) {
            unprocessedEMC = unprocessedEMC.add(generated.multiply(BigDecimal.valueOf(getSunLevel() / 16.0f)));
            if (unprocessedEMC.compareTo(BigDecimal.ONE) >= 0) {
                //Force add the EMC regardless of if we can receive EMC from external sources
                unprocessedEMC = unprocessedEMC.subtract(new BigDecimal(forceInsertEmcBigInteger(unprocessedEMC.toBigInteger(), EmcAction.EXECUTE)));
            }
            //Note: We don't need to recheck comparators because it doesn't take the unprocessed emc into account
            markDirty(level, pos, false);
        }

        if (getStoredEmcBigInteger().compareTo(BigInteger.ZERO) > 0) {
            ItemStack upgrading = getUpgrading();
            if (hasChargeableItem) {
                Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
                if (emcHolder.isPresent()) {
                    BigInteger toAdd = getStoredEmcBigInteger().min(transfer);
                    // insertEmc gives back what was taken, stepBigInteger wants what was left
                    BigInteger remaining = Util.stepBigInteger(toAdd, (val) -> val - emcHolder.get().insertEmc(upgrading, val, EmcAction.EXECUTE));
                    forceExtractEmcBigInteger(toAdd.subtract(remaining), EmcAction.EXECUTE);
                }
            } else if (hasFuel) {
                ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
                if (!fuelUpgrade.isEmpty()) {
                    ItemStack lock = getLock();
                    ItemStack result = lock.isEmpty() ? fuelUpgrade : lock.copy();

                    BigInteger upgradeCost = BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(result)).subtract(BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(upgrading)));

                    if (upgradeCost.compareTo(BigInteger.ZERO) >= 0 && this.getStoredEmcBigInteger().compareTo(upgradeCost) >= 0) {
                        ItemStack upgrade = getUpgraded();

                        if (getUpgraded().isEmpty()) {
                            forceExtractEmcBigInteger(upgradeCost, EmcAction.EXECUTE);
                            auxSlots.setStackInSlot(UPGRADE_SLOT, result);
                            upgrading.shrink(1);
                        } else if (result.getItem() == upgrade.getItem() && upgrade.getCount() < upgrade.getMaxStackSize()) {
                            forceExtractEmcBigInteger(upgradeCost, EmcAction.EXECUTE);
                            getUpgraded().grow(1);
                            upgrading.shrink(1);
                            auxSlots.onContentsChanged(UPGRADE_SLOT);
                        }
                    }
                }
            } else {
                BigInteger before = getStoredEmcBigInteger();
                // Only send EMC when we are not upgrading fuel or charging an item
                sendToAllAcceptors(level, pos, getStoredEmcBigInteger().min(transfer));
                BigInteger after = getStoredEmcBigInteger();
                sendRelayBonus(level, pos);
                if (!before.equals(after)) markDirty(level, pos, true);
            }
        }
    }

    @Override
    public BigInteger getMaximumEmcBigInteger() {
        boolean sunBonus = hasSunBonus();
        BigInteger max = BigInteger.valueOf(Fuel.getCollectorEMCLimit(Objects.requireNonNull(getMatter())));
        if (sunBonus) {
            max = max.multiply(BigInteger.valueOf(Objects.requireNonNull(getSunBonus())));
        }
        return max;
    }

    public long getEmcToNextGoal() {
        ItemStack lock = getLock();
        ItemStack upgrading = getUpgrading();
        long targetEmc;
        if (lock.isEmpty()) {
            targetEmc = ProjectEAPI.getEMCProxy().getValue(FuelMapper.getFuelUpgrade(upgrading));
        } else {
            targetEmc = ProjectEAPI.getEMCProxy().getValue(lock);
        }
        return Math.max(targetEmc - ProjectEAPI.getEMCProxy().getValue(upgrading), 0);
    }

    public long getItemCharge() {
        ItemStack upgrading = getUpgrading();
        if (!upgrading.isEmpty()) {
            Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
            return emcHolder.map(iItemEmcHolder -> iItemEmcHolder.getStoredEmc(upgrading)).orElse(-1L);
        }
        return -1;
    }

    public double getItemChargeProportion() {
        ItemStack upgrading = getUpgrading();
        long charge = getItemCharge();
        if (upgrading.isEmpty() || charge <= 0) {
            return -1;
        }
        Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
        if (emcHolder.isPresent()) {
            long max = emcHolder.get().getMaximumEmc(upgrading);
            if (charge >= max) {
                return 1;
            }
            return (double) charge / max;
        }
        return -1;
    }

    public int getSunLevel() {
        if (Objects.requireNonNull(level).dimensionType().ultraWarm()) {
            return 16;
        }
        return level.getMaxLocalRawBrightness(worldPosition.above()) + 1;
    }

    public double getFuelProgress() {
        if (getUpgrading().isEmpty() || !FuelMapper.isStackFuel(getUpgrading())) {
            return 0;
        }
        BigDecimal reqEmc;
        if (!getLock().isEmpty()) {
            reqEmc = BigDecimal.valueOf(ProjectEAPI.getEMCProxy().getValue(getLock())).subtract(BigDecimal.valueOf(ProjectEAPI.getEMCProxy().getValue(getUpgrading())));
            if (reqEmc.compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
        } else {
            if (FuelMapper.getFuelUpgrade(getUpgrading()).isEmpty()) {
                return 0;
            }
            reqEmc = BigDecimal.valueOf(ProjectEAPI.getEMCProxy().getValue(FuelMapper.getFuelUpgrade(getUpgrading()))).subtract(BigDecimal.valueOf(ProjectEAPI.getEMCProxy().getValue(getUpgrading())));
        }
        if (new BigDecimal(getStoredEmcBigInteger()).compareTo(reqEmc) >= 0) {
            return 1;
        }
        return new BigDecimal(getStoredEmcBigInteger()).divide(reqEmc, 3, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        input.deserializeNBT(tag.getCompound(TagNames.INPUT));
        auxSlots.deserializeNBT(tag.getCompound(TagNames.AUX_SLOTS));
        if (tag.contains(TagNames.UNPROCESSED_EMC, Constants.NBT.TAG_STRING)) unprocessedEMC = new BigDecimal(tag.getString(TagNames.UNPROCESSED_EMC));
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.put(TagNames.INPUT, input.serializeNBT());
        tag.put(TagNames.AUX_SLOTS, auxSlots.serializeNBT());
        tag.putString(TagNames.UNPROCESSED_EMC, unprocessedEMC.toString());
    }

    private void sendRelayBonus(World level, BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            TileEntity blockEntity = level.getBlockEntity(worldPosition.relative(dir));
            if (blockEntity instanceof BlockEntityRelay) {
                BlockEntityRelay be = ((BlockEntityRelay) blockEntity);
                be.addBonus(level, pos);
            }
            if (blockEntity instanceof RelayMK1Tile) {
                RelayMK1Tile be = ((RelayMK1Tile) blockEntity);
                // our blocks may only tick once per 20 ticks, so blocks other than ours must be ticked 20 times each time we tick to simulate regular ticking
                if (Config.server.enableCollectorOptimizations.get()) {
                    for (int i = 0; i < 20; i++) be.addBonus();
                } else {
                    be.addBonus();
                }
            }
        }
    }

    private void rotateUpgraded() {
        ItemStack upgraded = getUpgraded();
        if (!upgraded.isEmpty()) {
            if (getLock().isEmpty() || upgraded.getItem() != getLock().getItem() || upgraded.getCount() >= upgraded.getMaxStackSize()) {
                auxSlots.setStackInSlot(UPGRADE_SLOT, ItemHandlerHelper.insertItemStacked(input, upgraded.copy(), false));
            }
        }
    }

    private void checkFuelOrKlein() {
        ItemStack upgrading = getUpgrading();
        if (!upgrading.isEmpty()) {
            Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
            if (emcHolder.isPresent()) {
                if (emcHolder.get().getNeededEmc(upgrading) > 0) {
                    hasChargeableItem = true;
                    hasFuel = false;
                } else {
                    hasChargeableItem = false;
                }
            } else {
                hasFuel = FuelMapper.isStackFuel(upgrading);
                hasChargeableItem = false;
            }
        } else {
            hasFuel = false;
            hasChargeableItem = false;
        }
    }

    @Override
    public Matter getMatter() {
        // the inventory is sized before the matter is known, it gets resized once it is
        if (level == null) return matter == null ? Matter.BASIC : matter;
        BlockCollector block = (BlockCollector) getBlockState().getBlock();
        if (block.getMatter() != matter) {
            this.matter = block.getMatter();
        }
        return matter;
    }

    @Override
    public boolean hasSunBonus() {
        return BlockCompactSun.adjacent(level, worldPosition, Direction.UP);
    }

    @Override
    public BigInteger getGeneratedEMC() {
        return new BigDecimal(getMatter().getCollectorOutput())
                .multiply(BigDecimal.valueOf(getSunBonus() == null ? 1 : getSunBonus()))
                .multiply(BigDecimal.valueOf(getSunLevel() / 16.0f))
                .toBigInteger();
    }

    @Override
    public ITextComponent getDisplayName() {
        return Lang.Blocks.COLLECTOR.translate();
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        switch (getMatter()) {
            case BASIC: return new ContainerCollector.Tier1(windowId, playerInventory, this);
            case DARK: return new ContainerCollector.Tier2(windowId, playerInventory, this);
            default: return new ContainerCollector.Tier3(windowId, playerInventory, this);
        }
    }

    private class CollectorItemHandlerProvider extends SidedItemHandlerResolver {

        private final ICapabilityResolver<IItemHandler> automationAuxSlotsResolver;
        private final ICapabilityResolver<IItemHandler> automationInputResolved;
        private final ICapabilityResolver<IItemHandler> joinedResolver;

        protected CollectorItemHandlerProvider() {
            this.automationInputResolved = BasicCapabilityResolver.getBasicItemHandlerResolver(BlockEntityCollector.this.automationInput);
            this.automationAuxSlotsResolver = BasicCapabilityResolver.getBasicItemHandlerResolver(BlockEntityCollector.this.automationAuxSlots);
            this.joinedResolver = BasicCapabilityResolver.getBasicItemHandlerResolver(BlockEntityCollector.this.joined);
        }

        @Override
        protected ICapabilityResolver<IItemHandler> getResolver(@Nullable Direction side) {
            if (side == null) {
                return joinedResolver;
            } else if (side.getAxis().isVertical()) {
                return automationAuxSlotsResolver;
            }
            return automationInputResolved;
        }

        @Override
        public void invalidateAll() {
            joinedResolver.invalidateAll();
            automationInputResolved.invalidateAll();
            automationAuxSlotsResolver.invalidateAll();
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    @Override
    protected void serverTick() {
        tickServer(level, worldPosition, getBlockState(), this);
    }
}
