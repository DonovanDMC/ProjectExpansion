package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KnowledgeSyncPKT.class, remap = false)
public class KnowledgeSyncPKTMixin {
    @Inject(at = @At("HEAD"), method = "handle(Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V")
    public void handle(IPayloadContext context, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.transmutationInventory.updateClientTargets(false);
        }
    }
}
