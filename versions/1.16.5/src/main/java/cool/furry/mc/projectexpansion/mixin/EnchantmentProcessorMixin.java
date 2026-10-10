package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.nbt.processor.EnchantmentProcessor;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin makes it so only enchanted books keep their enchantments
@Mixin(value = EnchantmentProcessor.class, remap = false)
public class EnchantmentProcessorMixin {
    @Inject(at = @At("HEAD"), method = "getPersistentNBT(Lmoze_intel/projecte/api/ItemInfo;)Lnet/minecraft/nbt/CompoundNBT;", cancellable = true)
    public void getPersistentNBT(ItemInfo info, CallbackInfoReturnable<CompoundNBT> cir) {
        if (Config.server.persistEnchantedBooksOnly.get() && info.getItem() != Items.ENCHANTED_BOOK) {
            cir.setReturnValue(null);
        }
    }
}
