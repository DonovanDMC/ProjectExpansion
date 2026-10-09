package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.registries.Enchantments;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import moze_intel.projecte.utils.PEKeybind;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This mixin dips into ProjectE's extra function capability to toggle the Alchemical Collection enchantment
// I would much rather this actually make use of the capability properly, but we cannot access tags during capability registration,
// and we cannot trust all items to extend DiggerItem, or that we will not override other capabilities.
@Mixin(value = KeyPressPKT.class, remap = false)
public class AlchemicalCollectionToggleMixin {
    @Inject(at = @At("TAIL"), method = "handle(Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V", cancellable = true)
    public void handle(IPayloadContext context, CallbackInfo ci) {
        Player player = context.player();
        if (key == PEKeybind.EXTRA_FUNCTION) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (EnchantmentHelper.getTagEnchantmentLevel(player.registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION), stack) > 0) {
                    boolean currentValue = stack.getOrDefault(PEDataComponentTypes.ACTIVE, true);
                    stack.set(PEDataComponentTypes.ACTIVE, !currentValue);
                    ci.cancel();
                }
            }
        }
    }

    @Final
    @Shadow
    private PEKeybind key;
}
