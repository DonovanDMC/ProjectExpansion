package cool.furry.mc.forge.projectexpansion.mixin;

import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// a unique isFoil wouldn't be remapped in production, so this hooks the base method instead
@Mixin(Item.class)
public class TomeMixin {
    @Inject(at = @At("HEAD"), method = "isFoil(Lnet/minecraft/item/ItemStack;)Z", cancellable = true)
    public void isFoil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Tome) {
            cir.setReturnValue(true);
        }
    }
}
