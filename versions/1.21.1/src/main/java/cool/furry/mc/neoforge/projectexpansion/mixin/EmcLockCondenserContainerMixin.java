package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.EmcLock;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.CondenserMK2Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Stops EMC locked items from being placed in ProjectE's condenser input slots
// the target is the input slot validator lambda in initSlots
// the name isn't stable across ProjectE versions, so this is optional outside of dev
@Mixin(value = {CondenserContainer.class, CondenserMK2Container.class}, remap = false)
public class EmcLockCondenserContainerMixin {
    @Inject(at = @At("HEAD"), require = 0, method = "lambda$initSlots$0(Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true)
    private void isValidInput(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (EmcLock.isLocked(stack)) cir.setReturnValue(false);
    }
}
