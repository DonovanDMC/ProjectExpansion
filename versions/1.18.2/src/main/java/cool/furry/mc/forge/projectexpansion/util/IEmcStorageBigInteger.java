package cool.furry.mc.forge.projectexpansion.util;

import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;

public interface IEmcStorageBigInteger extends IEmcStorage {
    @Override
    @Range(from = 0L, to = Long.MAX_VALUE)
    default long getStoredEmc() {
        return Util.safeLongValue(getStoredEmcBigInteger());
    }

    @Override
    @Range(from = 0L, to = Long.MAX_VALUE)
    default long getMaximumEmc() {
        return Util.safeLongValue(getMaximumEmcBigInteger());
    }

    @Override
    @Range(from = 0L, to = Long.MAX_VALUE)
    default long getNeededEmc() {
        return Util.safeLongValue(getNeededEmcBigInteger());
    }

    @Override
    default boolean hasMaxedEmc() {
        return hasMaxedEmcBigInteger();
    }

    @Override
    default long extractEmc(long l, EmcAction emcAction) {
        return Util.safeLongValue(extractEmcBigInteger(BigInteger.valueOf(l), emcAction));
    }

    @Override
    default long insertEmc(long l, EmcAction emcAction) {
        return Util.safeLongValue(insertEmcBigInteger(BigInteger.valueOf(l), emcAction));
    }

    BigInteger getStoredEmcBigInteger();
    BigInteger getMaximumEmcBigInteger();
    default BigInteger getNeededEmcBigInteger() {
        return getMaximumEmcBigInteger().subtract(getStoredEmcBigInteger());
    }
    default boolean hasMaxedEmcBigInteger() {
        return getStoredEmcBigInteger().compareTo(getMaximumEmcBigInteger()) >= 0;
    }

    BigInteger extractEmcBigInteger(BigInteger value, EmcAction action);
    BigInteger insertEmcBigInteger(BigInteger value, EmcAction action);
}
