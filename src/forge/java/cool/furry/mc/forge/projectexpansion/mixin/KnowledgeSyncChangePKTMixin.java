package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KnowledgeSyncChangePKT.class, remap = false)
public class KnowledgeSyncChangePKTMixin {
    @Shadow
    @Final
    private boolean learned;

    @Shadow
    @Final
    private ItemInfo change;

    @Inject(at = @At("HEAD"), method = "handle(Lnet/minecraftforge/network/NetworkEvent$Context;)V")
    public void handle(NetworkEvent.Context context, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
        if (knowledge != null) {
            if (learned) {
                if (!knowledge.hasKnowledge(change) && knowledge.addKnowledge(change) && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
                    container.transmutationInventory.itemLearned();
                }
            } else if (knowledge.hasKnowledge(change) && knowledge.removeKnowledge(change) && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
                container.transmutationInventory.itemUnlearned();
            }
        }
    }
}
