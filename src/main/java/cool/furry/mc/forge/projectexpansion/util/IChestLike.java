package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.world.entity.player.Player;

public interface IChestLike {
	void startOpen(Player player);
	void stopOpen(Player player);
	void recheckOpen();
	float getOpenNess(float partialTicks);
}
