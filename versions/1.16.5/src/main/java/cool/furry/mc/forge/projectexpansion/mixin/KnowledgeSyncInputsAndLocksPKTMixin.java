package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;
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

    @Inject(at = @At("HEAD"), method = "handle(Lnet/minecraftforge/fml/network/NetworkEvent$Context;)V")
    public void handle(NetworkEvent.Context context, CallbackInfo ci) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        IKnowledgeProvider knowledge = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (knowledge != null) {
            if (updateTargets != IKnowledgeProvider.TargetUpdateType.NONE && player.containerMenu instanceof ContainerArcaneTransmutationTablet) {
                ContainerArcaneTransmutationTablet container = ((ContainerArcaneTransmutationTablet) player.containerMenu);
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
