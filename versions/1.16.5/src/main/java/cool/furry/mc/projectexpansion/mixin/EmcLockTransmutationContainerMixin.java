package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.util.EmcLock;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Shift clicking in the transmutation table burns items without going through the burn slot, so it needs its own check
@Mixin(TransmutationContainer.class)
public class EmcLockTransmutationContainerMixin {
    @Inject(at = @At("HEAD"), method = "quickMoveStack(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;", cancellable = true)
    public void quickMoveStack(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        Container menu = (Container) (Object) this;
        if (index < 0 || index >= menu.slots.size()) return;
        Slot slot = menu.slots.get(index);
        if (slot.container instanceof PlayerInventory && EmcLock.isLocked(slot.getItem())) cir.setReturnValue(ItemStack.EMPTY);
    }
}
