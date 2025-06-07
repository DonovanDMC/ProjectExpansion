package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerCollector;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.capability.managing.BasicCapabilityResolver;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import moze_intel.projecte.capability.managing.SidedItemHandlerResolver;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.block_entities.RelayMK1BlockEntity;
import moze_intel.projecte.gameObjs.block_entities.WrappedItemHandler;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class BlockEntityCollector extends BlockEntityEMC implements IHasMatter, IHasSunBonus, IGeneratesEMC, MenuProvider {
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

    private boolean hasChargeableItem;
    private boolean hasFuel;
    private BigDecimal unprocessedEMC = BigDecimal.ZERO;
    //Start as needing to check for compacting when loaded
    private boolean needsCompacting = true;
    private Matter matter;
    public BlockEntityCollector(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.COLLECTOR.get(), pos, state);
        this.itemHandlerResolver = new CollectorItemHandlerProvider();
        resetStackHandlers();
    }

    private void resetStackHandlers() {
        input.setSize(getInvSize());
        auxSlots.setSize(3);
    }

    @Override
    protected boolean canAcceptEmc() {
        //Collector accepts EMC from providers if it has fuel/chargeable. Otherwise it sends it to providers
        return hasFuel || hasChargeableItem;
    }

    public IItemHandler getInput() {
        return input;
    }

    public IItemHandler getAux() {
        return auxSlots;
    }

    protected int getInvSize() {
        return Math.max(12, (getMatter().ordinal() + 1) * 4) + 4;
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

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityCollector be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityCollector blockEntity) {
        if (Config.enableCollectorOptimizations.get() && (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        if (needsCompacting) {
            ItemHelper.compactInventory(toSort);
            needsCompacting = false;
		}
        checkFuelOrKlein();
        updateEmc();
        rotateUpgraded();
        updateComparators();
    }

    private void updateEmc() {
        BigDecimal gen = getMatter().getCollectorOutputForTicks(Config.enableCollectorOptimizations.get() ? 20 : 1);
        if(hasSunBonus() && getSunBonus() != null) {
            gen = gen.multiply(BigDecimal.valueOf(getSunBonus()));
        }
        final BigDecimal generated = gen; // Thanks Java
        if (!this.hasMaxedEmc()) {
            unprocessedEMC = unprocessedEMC.add(generated.multiply(BigDecimal.valueOf(getSunLevel() / 16.0f)));
            if (unprocessedEMC.compareTo(BigDecimal.ONE) >= 0) {
                //Force add the EMC regardless of if we can receive EMC from external sources
                unprocessedEMC = unprocessedEMC.subtract(new BigDecimal(forceInsertEmcBigInteger(unprocessedEMC.toBigInteger(), EmcAction.EXECUTE)));
            }
            //Note: We don't need to recheck comparators because it doesn't take the unprocessed emc into account
            markDirty(false);
        }

        if (getStoredEmcBigInteger().compareTo(BigInteger.ZERO) > 0) {
            ItemStack upgrading = getUpgrading();
            if (hasChargeableItem) {
                upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).ifPresent(emcHolder -> {
                    BigInteger remaining = Util.stepBigInteger((getStoredEmcBigInteger().min(generated.toBigInteger())), (val) -> emcHolder.insertEmc(upgrading, val, EmcAction.EXECUTE));
                    BigInteger v = getStoredEmcBigInteger().subtract(remaining);
                    forceExtractEmcBigInteger(v, EmcAction.EXECUTE);
                });
            } else if (hasFuel) {
                ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
                if (!fuelUpgrade.isEmpty()) {
                    ItemStack lock = getLock();
                    ItemStack result = lock.isEmpty() ? fuelUpgrade : lock.copy();

                    BigInteger upgradeCost = BigInteger.valueOf(EMCHelper.getEmcValue(result)).subtract(BigInteger.valueOf(EMCHelper.getEmcValue(upgrading)));

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
                // Only send EMC when we are not upgrading fuel or charging an item
                BigInteger toSend = getStoredEmcBigInteger().compareTo(generated.toBigInteger()) < 0 ? getStoredEmcBigInteger() : generated.toBigInteger();
                sendToAllAcceptors(toSend);
                sendRelayBonus();
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
            targetEmc = EMCHelper.getEmcValue(FuelMapper.getFuelUpgrade(upgrading));
        } else {
            targetEmc = EMCHelper.getEmcValue(lock);
        }
        return Math.max(targetEmc - EMCHelper.getEmcValue(upgrading), 0);
    }

    public long getItemCharge() {
        ItemStack upgrading = getUpgrading();
        if (!upgrading.isEmpty()) {
            return upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).map(emcHolder -> emcHolder.getStoredEmc(upgrading)).orElse(-1L);
        }
        return -1;
    }

    public double getItemChargeProportion() {
        ItemStack upgrading = getUpgrading();
        long charge = getItemCharge();
        if (upgrading.isEmpty() || charge <= 0) {
            return -1;
        }
        Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
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
            reqEmc = BigDecimal.valueOf(EMCHelper.getEmcValue(getLock())).subtract(BigDecimal.valueOf(EMCHelper.getEmcValue(getUpgrading())));
            if (reqEmc.compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
        } else {
            if (FuelMapper.getFuelUpgrade(getUpgrading()).isEmpty()) {
                return 0;
            }
            reqEmc = BigDecimal.valueOf(EMCHelper.getEmcValue(FuelMapper.getFuelUpgrade(getUpgrading()))).subtract(BigDecimal.valueOf(EMCHelper.getEmcValue(getUpgrading())));
        }
        if (new BigDecimal(getStoredEmcBigInteger()).compareTo(reqEmc) >= 0) {
            return 1;
        }
        return new BigDecimal(getStoredEmcBigInteger()).divide(reqEmc, 3, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        input.deserializeNBT(nbt.getCompound(TagNames.INPUT));
        auxSlots.deserializeNBT(nbt.getCompound(TagNames.AUX_SLOTS));
        unprocessedEMC = new BigDecimal(nbt.getString(TagNames.UNPROCESSED_EMC));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(TagNames.INPUT, input.serializeNBT());
        tag.put(TagNames.AUX_SLOTS, auxSlots.serializeNBT());
        tag.putString(TagNames.UNPROCESSED_EMC, unprocessedEMC.toString());
    }

    private void sendRelayBonus() {
        if (level == null) return;

        for (Direction dir : DIRECTIONS) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof RelayMK1BlockEntity b) {
                for (int i = 0; i < 20; i++) b.addBonus();
                Util.markDirty(b);
            } else if (be instanceof BlockEntityRelay b) {
                b.addBonus();
                Util.markDirty(b);
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
            Optional<IItemEmcHolder> emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
            if (emcHolder.isPresent()) {
                if (emcHolder.get().getNeededEmc(upgrading) > 0) {
                    hasChargeableItem = true;
                    hasFuel = false;
                } else {
                    hasChargeableItem = false;
                }
            } else {
                hasFuel = true;
                hasChargeableItem = false;
            }
        } else {
            hasFuel = false;
            hasChargeableItem = false;
        }
    }

    @Override
    public @NotNull Matter getMatter() {
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
    public Component getDisplayName() {
        return Lang.Blocks.COLLECTOR.translate();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return switch (getMatter()) {
            case BASIC -> new ContainerCollector.Tier1(windowId, playerInventory, this);
            case DARK -> new ContainerCollector.Tier2(windowId, playerInventory, this);
            default -> new ContainerCollector.Tier3(windowId, playerInventory, this);
        };
    }

    private class CollectorItemHandlerProvider extends SidedItemHandlerResolver {

        private final ICapabilityResolver<IItemHandler> automationAuxSlots;
        private final ICapabilityResolver<IItemHandler> automationInput;
        private final ICapabilityResolver<IItemHandler> joined;

        protected CollectorItemHandlerProvider() {
            NonNullLazy<IItemHandler> automationInput = NonNullLazy.of(() -> new WrappedItemHandler(input, WrappedItemHandler.WriteMode.IN) {
                @NotNull
                @Override
                public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    return SlotPredicates.COLLECTOR_INV.test(stack) ? super.insertItem(slot, stack, simulate) : stack;
                }
            });
            NonNullLazy<IItemHandler> automationAuxSlots = NonNullLazy.of(() -> new WrappedItemHandler(auxSlots, WrappedItemHandler.WriteMode.OUT) {
                @NotNull
                @Override
                public ItemStack extractItem(int slot, int count, boolean simulate) {
                    if (slot == UPGRADE_SLOT) {
                        return super.extractItem(slot, count, simulate);
                    }
                    return ItemStack.EMPTY;
                }
            });
            this.automationInput = BasicCapabilityResolver.getBasicItemHandlerResolver(automationInput);
            this.automationAuxSlots = BasicCapabilityResolver.getBasicItemHandlerResolver(automationAuxSlots);
            this.joined = BasicCapabilityResolver.getBasicItemHandlerResolver(() -> new CombinedInvWrapper((IItemHandlerModifiable) automationInput.get(),
                    (IItemHandlerModifiable) automationAuxSlots.get()));
        }

        @Override
        protected ICapabilityResolver<IItemHandler> getResolver(@Nullable Direction side) {
            if (side == null) {
                return joined;
            } else if (side.getAxis().isVertical()) {
                return automationAuxSlots;
            }
            return automationInput;
        }

        @Override
        public void invalidateAll() {
            joined.invalidateAll();
            automationInput.invalidateAll();
            automationAuxSlots.invalidateAll();
        }
    }
}
