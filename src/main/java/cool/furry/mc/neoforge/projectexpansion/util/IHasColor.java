package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.world.item.DyeColor;

import javax.annotation.Nonnull;

public interface IHasColor {
	@Nonnull
	@SuppressWarnings("unused")
	DyeColor getColor();
}
