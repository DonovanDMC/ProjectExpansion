package cool.furry.mc.neoforge.projectexpansion.mixin;

import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Makes the Tome of Knowledge have the enchantment glint, because I want it to dammit
@SuppressWarnings("unused")
@Mixin(Tome.class)
public class TomeMixin {
    @SuppressWarnings("SameReturnValue")
    @Unique
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
