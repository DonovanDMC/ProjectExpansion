package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClearKnowledgePacket implements IPacket {
    public static final ClearKnowledgePacket INSTANCE = new ClearKnowledgePacket();
    public static final CustomPacketPayload.Type<ClearKnowledgePacket> TYPE = new CustomPacketPayload.Type<>(Main.rl("clear_knowledge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearKnowledgePacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.transmutationInventory.updateClientTargets(false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}