package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.nbt.processor.EnchantmentProcessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentProcessor.class)
public class EnchantmentProcessorMixin {
    @Inject(at = @At("HEAD"), method = "getPersistentNBT(Lmoze_intel/projecte/api/ItemInfo;)Lnet/minecraft/nbt/CompoundTag;", cancellable = true, remap = false)
    public void getPersistentNBT(ItemInfo info, CallbackInfoReturnable<CompoundTag> cir) {
        if(Config.persistEnchantedBooksOnly.get() && info.getItem() != Items.ENCHANTED_BOOK) {
            cir.setReturnValue(null);
        }
    }
}
