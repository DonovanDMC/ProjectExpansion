package cool.furry.mc.projectexpansion.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.math.BigInteger;

// ProjectE's IRelay only exists from 1.21, so this declares everything itself
public interface IRelayBigInteger {
    default double getBonusToAdd() {
        return Util.safeLongValue(getBonusToAddBigInteger());
    }

    BigInteger getBonusToAddBigInteger();

    void addBonus(World level, BlockPos pos);
}
