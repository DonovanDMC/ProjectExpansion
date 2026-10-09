package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SearchUpdatePKT.class, remap = false)
public class SearchUpdatePKTMixin {
    @Shadow
    @Final
    private int slot;

    @Shadow
    @Final
    private ItemStack itemStack;

    @Inject(at = @At("HEAD"), method = "handle(Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V")
    public void handle(IPayloadContext context, CallbackInfo ci) {
        if (context.player().containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.transmutationInventory.writeIntoOutputSlot(slot, itemStack);
        }
    }
}
