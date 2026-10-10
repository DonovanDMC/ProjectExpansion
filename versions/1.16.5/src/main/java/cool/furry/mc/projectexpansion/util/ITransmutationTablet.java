package cool.furry.mc.projectexpansion.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

// TODO: replace with api version when https://github.com/sinkillerj/ProjectE/pull/2440 is merged
public interface ITransmutationTablet {
    void openContainer(PlayerEntity player, Hand hand, int selected);
    void openContainer(PlayerEntity player);
}
