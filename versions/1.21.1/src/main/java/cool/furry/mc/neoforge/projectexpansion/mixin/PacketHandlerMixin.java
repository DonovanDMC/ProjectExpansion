package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.ClearKnowledgePacket;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.UpdateTransmutationTargetsPacket;
import moze_intel.projecte.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketHandler.class, remap = false)
public class PacketHandlerMixin {
    @Inject(at = @At("TAIL"), method = "clearKnowledge(Lnet/minecraft/server/level/ServerPlayer;)V")
    public void clearKnowledge(ServerPlayer player, CallbackInfo ci) {
        PacketDistributor.sendToPlayer(player, ClearKnowledgePacket.INSTANCE);
    }

    @Inject(at = @At("TAIL"), method = "updateTransmutationTargets(Lnet/minecraft/server/level/ServerPlayer;)V")
    public void updateTransmutationTargets(ServerPlayer player, CallbackInfo ci) {
        PacketDistributor.sendToPlayer(player, UpdateTransmutationTargetsPacket.INSTANCE);
    }
}
