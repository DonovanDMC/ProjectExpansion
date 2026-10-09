package cool.furry.mc.neoforge.projectexpansion.util;

import moze_intel.projecte.api.block_entity.IRelay;

import java.math.BigInteger;

public interface IRelayBigInteger extends IRelay {
    default double getBonusToAdd() {
        return Util.safeLongValue(getBonusToAddBigInteger());
    }

    BigInteger getBonusToAddBigInteger();
}
