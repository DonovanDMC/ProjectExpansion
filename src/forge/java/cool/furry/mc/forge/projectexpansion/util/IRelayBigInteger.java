package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.math.BigInteger;

// ProjectE's IRelay only exists from 1.21, so this declares everything itself
public interface IRelayBigInteger {
    default double getBonusToAdd() {
        return Util.safeLongValue(getBonusToAddBigInteger());
    }

    BigInteger getBonusToAddBigInteger();

    void addBonus(Level level, BlockPos pos);
}
