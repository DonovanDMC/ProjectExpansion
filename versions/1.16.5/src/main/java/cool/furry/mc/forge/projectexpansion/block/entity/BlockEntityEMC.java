package cool.furry.mc.forge.projectexpansion.block.entity;

import com.mojang.datafixers.util.Either;
import cool.furry.mc.forge.projectexpansion.registries.Capabilities;
import cool.furry.mc.forge.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class BlockEntityEMC extends BlockEntityBase implements IEmcStorageBigInteger {
    private BigInteger maximumEMC;
    private BigInteger emc = BigInteger.ZERO;
    public static final Direction[] DIRECTIONS = Direction.values();

    public BlockEntityEMC(TileEntityType<?> type) {
        this(type, BigInteger.valueOf(Long.MAX_VALUE));
        this.addCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, (side) -> this);
        this.addCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, (side) -> this);
    }

    public BlockEntityEMC(TileEntityType<?> type, BigInteger maximumEMC) {
        super(type);
        setMaximumEMC(maximumEMC);
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

    protected void sendToAllAcceptors(World level, BlockPos pos, BigInteger emc) {
        sendToAllAcceptors(level, pos, emc, null);
    }

    @SuppressWarnings("SameParameterValue")
    protected void sendToAllAcceptors(World level, BlockPos pos, BigInteger emc, @Nullable BigInteger transferLimit) {
        if (emc.equals(BigInteger.ZERO) || !canProvideEmc()) {
            return;
        }
        emc = getEmcExtractLimitBigInteger().min(emc);

        List<IEmcStorage> targets = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            Either<IEmcStorage, IEmcStorageBigInteger> anyStorage = null;
            IEmcStorageBigInteger bigStorage = Util.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, level, pos.relative(dir), dir.getOpposite()).orElse(null);
            if (bigStorage == null) {
                IEmcStorage storage = Util.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, level, pos.relative(dir), dir.getOpposite()).orElse(null);
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
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putString(TagNames.STORED_EMC, getStoredEmcBigInteger().toString());
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        emc = new BigInteger(tag.getString(TagNames.STORED_EMC));
    }
}
