package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Stops automation from inserting EMC locked items into ProjectE's condensers
// these are the anonymous input handlers created in createAutomationInventory
// the names aren't stable across ProjectE versions, so this is optional outside of dev
@Pseudo
@Mixin(targets = {
        "moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity$1",
        "moze_intel.projecte.gameObjs.block_entities.CondenserMK2BlockEntity$1"
}, remap = false)
public class EmcLockCondenserMixin {
    @Inject(at = @At("HEAD"), require = 0, method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", cancellable = true)
    public void insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (EmcLock.isLocked(stack)) cir.setReturnValue(stack);
    }
}
