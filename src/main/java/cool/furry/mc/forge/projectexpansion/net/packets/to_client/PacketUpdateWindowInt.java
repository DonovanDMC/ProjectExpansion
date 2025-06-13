package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateWindowInt(short windowId, short propId, int propVal) implements IPacket {
    public static final CustomPacketPayload.Type<PacketUpdateWindowInt> TYPE = new CustomPacketPayload.Type<>(Main.rl("update_window_int"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateWindowInt> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.SHORT, PacketUpdateWindowInt::windowId,
        ByteBufCodecs.SHORT, PacketUpdateWindowInt::propId,
        ByteBufCodecs.INT, PacketUpdateWindowInt::propVal,
        PacketUpdateWindowInt::new
    );

    @Override
    public void handle(IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarInt(propId, propVal);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}