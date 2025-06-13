package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.components.processor.EnchantmentProcessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin makes it so only enchanted books keep their enchantments
@Mixin(EnchantmentProcessor.class)
public class EnchantmentProcessorMixin {
    @Inject(at = @At("HEAD"), method = "shouldPersist(Lmoze_intel/projecte/api/ItemInfo;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)Z", cancellable = true, remap = false)
    public void shouldPersist(ItemInfo info, ItemEnchantments component, CallbackInfoReturnable<Boolean> cir) {
        if(Config.server.persistEnchantedBooksOnly.get() && info.getItem() != Items.ENCHANTED_BOOK) {
            cir.setReturnValue(false);
        }
    }
}
