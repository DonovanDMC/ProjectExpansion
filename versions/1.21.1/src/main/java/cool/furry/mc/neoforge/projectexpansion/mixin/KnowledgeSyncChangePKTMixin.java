package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

    @Inject(at = @At("HEAD"), method = "handle(Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V")
    public void handle(IPayloadContext context, CallbackInfo ci) {
        Player player = context.player();
        IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (knowledge != null) {
            if (learned) {
                if (!knowledge.hasKnowledge(change) && knowledge.addKnowledge(change) && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
                    container.transmutationInventory.itemLearned(change);
                }
            } else if (knowledge.hasKnowledge(change) && knowledge.removeKnowledge(change) && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
                container.transmutationInventory.itemUnlearned(change);
            }
        }
    }
}
