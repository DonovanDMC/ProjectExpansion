package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.util.EmcLock;
import net.minecraft.item.ItemStack;
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
        "moze_intel.projecte.gameObjs.tiles.CondenserTile$1",
        "moze_intel.projecte.gameObjs.tiles.CondenserMK2Tile$1"
}, remap = false)
public class EmcLockCondenserMixin {
    @Inject(at = @At("HEAD"), require = 0, method = "insertItem(ILnet/minecraft/item/ItemStack;Z)Lnet/minecraft/item/ItemStack;", cancellable = true)
    public void insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (EmcLock.isLocked(stack)) cir.setReturnValue(stack);
    }
}
