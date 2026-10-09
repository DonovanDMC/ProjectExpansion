package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public interface IChestLike {
	void startOpen(PlayerEntity player);
	void stopOpen(PlayerEntity player);
	void recheckOpen();
	float getOpenNess(float partialTicks);
}
