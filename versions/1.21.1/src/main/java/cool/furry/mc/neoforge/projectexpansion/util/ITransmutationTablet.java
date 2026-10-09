package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

// TODO: replace with api version when https://github.com/sinkillerj/ProjectE/pull/2440 is merged
public interface ITransmutationTablet {
    void openContainer(Player player, InteractionHand hand, int selected);
    void openContainer(Player player);
}
