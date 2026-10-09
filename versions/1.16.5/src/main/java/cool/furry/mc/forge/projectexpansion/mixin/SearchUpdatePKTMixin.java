package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.NetworkEvent;
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

    @Inject(at = @At("HEAD"), method = "handle(Lnet/minecraftforge/fml/network/NetworkEvent$Context;)V")
    public void handle(NetworkEvent.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
            ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
            container.transmutationInventory.writeIntoOutputSlot(slot, itemStack);
        }
    }
}
