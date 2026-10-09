package cool.furry.mc.neoforge.projectexpansion.block.entity;

import com.mojang.datafixers.util.Either;
import cool.furry.mc.neoforge.projectexpansion.registries.Capabilities;
import cool.furry.mc.neoforge.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.neoforge.projectexpansion.util.TagNames;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class BlockEntityEMC extends BlockEntityBase implements IEmcStorageBigInteger {
    public static final ICapabilityProvider<BlockEntityEMC, @Nullable Direction, IEmcStorage> EMC_STORAGE_PROVIDER = (be, side) -> be;
    public static final ICapabilityProvider<BlockEntityEMC, @Nullable Direction, IEmcStorageBigInteger> BIG_EMC_STORAGE_PROVIDER = (be, side) -> be;
    private BigInteger maximumEMC;
    private BigInteger emc = BigInteger.ZERO;
    public static final Direction[] DIRECTIONS = Direction.values();

    public BlockEntityEMC(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, BigInteger.valueOf(Long.MAX_VALUE));
    }

    public BlockEntityEMC(BlockEntityType<?> type, BlockPos pos, BlockState state, BigInteger maximumEMC) {
        super(type, pos, state);
        setMaximumEMC(maximumEMC);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<? extends BlockEntityEMC> type) {
        event.registerBlockEntity(PECapabilities.EMC_STORAGE_CAPABILITY, type, EMC_STORAGE_PROVIDER);
        event.registerBlockEntity(Capabilities.BIG_EMC_STORAGE_CAPABILITY, type, BIG_EMC_STORAGE_PROVIDER);
    }

    /*******
     * EMC *
     *******/

    protected boolean emcAffectsComparators() {
        return false;
    }

    public final void setMaximumEMC(BigInteger max) {
        maximumEMC = max;
        if (getStoredEmcBigInteger().compareTo(getMaximumEmcBigInteger()) > 0) {
            emc = getMaximumEmcBigInteger();
            storedEmcChanged();
        }
    }

    @Override
    public BigInteger getStoredEmcBigInteger() {
        return emc;
    }

    @Override
    public BigInteger getMaximumEmcBigInteger() {
        return maximumEMC;
    }

    @Range(from = 0, to = Long.MAX_VALUE)
    protected long getEmcInsertLimit() {
        return getNeededEmc();
    }

    protected BigInteger getEmcInsertLimitBigInteger() {
        return getNeededEmcBigInteger();
    }

    @Range(from = 0, to = Long.MAX_VALUE)
    protected long getEmcExtractLimit() {
        return getStoredEmc();
    }

    protected BigInteger getEmcExtractLimitBigInteger() {
        return getStoredEmcBigInteger();
    }

    protected boolean canAcceptEmc() {
        return true;
    }

    @SuppressWarnings("SameReturnValue")
    protected boolean canProvideEmc() {
        return true;
    }

    @Override
    public BigInteger extractEmcBigInteger(BigInteger toExtract, EmcAction action) {
        if (toExtract.compareTo(BigInteger.ZERO) < 0) {
            return insertEmcBigInteger(toExtract.negate(), action);
        }
        if (canProvideEmc()) {
            return forceExtractEmcBigInteger(getStoredEmcBigInteger().min(toExtract), action);
        }
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger insertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.compareTo(BigInteger.ZERO) < 0) {
            return extractEmcBigInteger(toAccept.negate(), action);
        }
        if (canAcceptEmc()) {
            return forceInsertEmcBigInteger(getEmcInsertLimitBigInteger().min(toAccept), action);
        }
        return BigInteger.ZERO;
    }

    protected long forceExtractEmc(long toExtract, EmcAction action) {
        return Util.safeLongValue(forceExtractEmcBigInteger(BigInteger.valueOf(toExtract), action));
    }

    protected BigInteger forceExtractEmcBigInteger(BigInteger toExtract, EmcAction action) {
        if (toExtract.compareTo(BigInteger.ZERO) < 0) {
            return forceInsertEmcBigInteger(toExtract.negate(), action);
        }
        BigInteger toRemove = getStoredEmcBigInteger().min(toExtract);
        if (action.execute()) {
            emc = emc.subtract(toRemove);
            storedEmcChanged();
        }

        return toRemove;
    }

    protected long forceInsertEmc(long toAccept, EmcAction action) {
        return Util.safeLongValue(forceInsertEmcBigInteger(BigInteger.valueOf(toAccept), action));
    }

    protected BigInteger forceInsertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.compareTo(BigInteger.ZERO) < 0) {
            return forceExtractEmcBigInteger(toAccept.negate(), action);
        }
        BigInteger toAdd = getNeededEmcBigInteger().min(toAccept);
        if (action.execute()) {
            emc = emc.add(toAdd);
            storedEmcChanged();
        }
        return toAdd;
    }

    protected void sendToAllAcceptors(Level level, BlockPos pos, BigInteger emc) {
        sendToAllAcceptors(level, pos, emc, null);
    }

    @SuppressWarnings("SameParameterValue")
    protected void sendToAllAcceptors(Level level, BlockPos pos, BigInteger emc, @Nullable BigInteger transferLimit) {
        if (emc.equals(BigInteger.ZERO) || !canProvideEmc()) {
            return;
        }
        emc = getEmcExtractLimitBigInteger().min(emc);

        List<IEmcStorage> targets = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            Either<IEmcStorage, IEmcStorageBigInteger> anyStorage = null;
            IEmcStorageBigInteger bigStorage = WorldHelper.getCapability(level, Capabilities.BIG_EMC_STORAGE_CAPABILITY, pos.relative(dir), dir.getOpposite());
            if (bigStorage == null) {
                IEmcStorage storage = WorldHelper.getCapability(level, PECapabilities.EMC_STORAGE_CAPABILITY, pos.relative(dir), dir.getOpposite());
                if (storage != null) {
                    anyStorage = Either.left(storage);
                }
            } else {
                anyStorage = Either.right(bigStorage);
            }

            if (anyStorage != null) {
                // If they are both relays don't add the pairing to prevent thrashing
                // If they are wiling to accept any Emc then we consider them to be an "acceptor"
                anyStorage.ifLeft(storage -> {
                    if ((!isRelay() || !storage.isRelay()) && storage.insertEmc(1, EmcAction.SIMULATE) > 0) {
                        targets.add(storage);
                    }
                });

                anyStorage.ifRight(storage -> {
                    if ((!isRelay() || !storage.isRelay()) && storage.insertEmcBigInteger(BigInteger.ONE, EmcAction.SIMULATE).compareTo(BigInteger.ZERO) > 0) {
                        targets.add(storage);
                    }
                });
            }
        }


        BigInteger remaining = Util.spreadEMC(emc, targets, transferLimit);
        forceExtractEmcBigInteger(emc.subtract(remaining), EmcAction.EXECUTE);
    }


    protected void storedEmcChanged() {
        if (level != null) {
            markDirty(level, worldPosition, emcAffectsComparators());
        }
    }


    /*******
     * NBT *
     *******/

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TagNames.STORED_EMC, getStoredEmcBigInteger().toString());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        emc = new BigInteger(tag.getString(TagNames.STORED_EMC));
    }
}
