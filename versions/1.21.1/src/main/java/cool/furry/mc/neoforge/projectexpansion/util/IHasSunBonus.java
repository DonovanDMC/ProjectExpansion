package cool.furry.mc.neoforge.projectexpansion.util;

import javax.annotation.Nullable;

public interface IHasSunBonus {
    boolean hasSunBonus();

    default @Nullable Integer getSunBonus() {
        if (!hasSunBonus()) return null;
        int bonus = Util.getSunBonus();
        return bonus == 0 ? null : bonus;
    }
}
