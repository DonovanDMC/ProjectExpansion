package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeClearPKT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ProjectE only updates its own transmutation container
@Mixin(value = KnowledgeClearPKT.class, remap = false)
public class KnowledgeClearPKTMixin {
    @Inject(at = @At("TAIL"), method = "handle(Lnet/minecraftforge/fml/network/NetworkEvent$Context;)V")
    public void handle(NetworkEvent.Context context, CallbackInfo ci) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
            ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
            container.transmutationInventory.updateClientTargets();
        }
    }
}
