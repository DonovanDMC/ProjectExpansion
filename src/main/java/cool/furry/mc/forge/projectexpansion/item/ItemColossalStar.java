package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.Star;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemColossalStar extends ItemMagnumStar {
    public ItemColossalStar(Star t) {
        super(t, 2);
    }

    @Override
    public long getMaximumEmc(@Nonnull ItemStack stack) {
        return STAR_EMC[tier.ordinal() + 6];
    }
}

