package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.registries.Enchantments;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
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

@Mixin(KeyPressPKT.class)
public class AlchemicalCollectionToggleMixin {
    @Inject(at = @At("HEAD"), method = "tryPerformCapability(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraftforge/common/util/NonNullPredicate;)Z", cancellable = true, remap = false)
    private static <CAPABILITY> void tryPerformCapability(ItemStack stack, Capability<CAPABILITY> capability, NonNullPredicate<CAPABILITY> perform, CallbackInfoReturnable<Boolean> cir) {
        if(capability == ProjectEAPI.EXTRA_FUNCTION_ITEM_CAPABILITY) {
            boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
            if(hasEnch) {
                boolean currentValue = stack.getOrCreateTag().getBoolean(NBTNames.ALCHEMICAL_COLLECTION_ENABLED);
                stack.getOrCreateTag().putBoolean(NBTNames.ALCHEMICAL_COLLECTION_ENABLED, !currentValue);
                cir.setReturnValue(true);
            }
        }
    }
}
