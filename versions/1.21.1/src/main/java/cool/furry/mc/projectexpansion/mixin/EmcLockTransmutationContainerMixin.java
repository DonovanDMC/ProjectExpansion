package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.util.EmcLock;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Shift clicking in the transmutation table burns items without going through the burn slot, so it needs its own check
@Mixin(value = TransmutationContainer.class, remap = false)
public class EmcLockTransmutationContainerMixin {
    @Inject(at = @At("HEAD"), method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;", cancellable = true)
    public void quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) return;
        Slot slot = menu.slots.get(index);
        if (slot.container instanceof Inventory && EmcLock.isLocked(slot.getItem())) cir.setReturnValue(ItemStack.EMPTY);
    }
}
