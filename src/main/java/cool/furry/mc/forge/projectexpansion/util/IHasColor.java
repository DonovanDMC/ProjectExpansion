package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.world.item.DyeColor;

import javax.annotation.Nonnull;

public interface IHasColor {
	@Nonnull
	@SuppressWarnings("unused")
	DyeColor getColor();
}
