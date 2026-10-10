package cool.furry.mc.projectexpansion.mixin;

import cool.furry.mc.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KnowledgeSyncInputsAndLocksPKT.class, remap = false)
public class KnowledgeSyncInputsAndLocksPKTMixin {
    @Shadow
    @Final
    private IKnowledgeProvider.TargetUpdateType updateTargets;

    @Inject(at = @At("HEAD"), method = "handle(Lnet/minecraftforge/network/NetworkEvent$Context;)V")
    public void handle(NetworkEvent.Context context, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (knowledge != null) {
            if (updateTargets != IKnowledgeProvider.TargetUpdateType.NONE && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
                TransmutationInventory transmutationInventory = container.transmutationInventory;
                if (updateTargets == IKnowledgeProvider.TargetUpdateType.ALL) {
                    transmutationInventory.updateClientTargets();
                } else {
                    transmutationInventory.checkForUpdates();
                }
            }
        }
    }
}
