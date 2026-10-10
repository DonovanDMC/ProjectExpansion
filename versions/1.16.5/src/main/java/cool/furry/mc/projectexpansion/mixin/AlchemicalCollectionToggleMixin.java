package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.registries.Enchantments;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.NonNullPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// This mixin dips into ProjectE's extra function capability to toggle the Alchemical Collection enchantment
// I would much rather this actually make use of the capability properly, but we cannot access tags during capability registration,
// and we cannot trust all items to extend DiggerItem, or that we will not override other capabilities.
@Mixin(value = KeyPressPKT.class, remap = false)
public class AlchemicalCollectionToggleMixin {
    @Inject(at = @At("HEAD"), method = "tryPerformCapability(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraftforge/common/util/NonNullPredicate;)Z", cancellable = true)
    private static <CAPABILITY> void tryPerformCapability(ItemStack stack, Capability<CAPABILITY> capability, NonNullPredicate<CAPABILITY> perform, CallbackInfoReturnable<Boolean> cir) {
        if (capability == ProjectEAPI.EXTRA_FUNCTION_ITEM_CAPABILITY && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0) {
            Util.setActive(stack, !Util.isActive(stack, true));
            cir.setReturnValue(true);
        }
    }
}
