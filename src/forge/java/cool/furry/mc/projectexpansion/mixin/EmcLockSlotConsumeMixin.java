package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.util.EmcLock;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Stops EMC locked items from being placed in the transmutation burn slot
@Mixin(SlotConsume.class)
public class EmcLockSlotConsumeMixin {
    @Inject(at = @At("HEAD"), method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true)
    public void mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (EmcLock.isLocked(stack)) cir.setReturnValue(false);
    }
}
