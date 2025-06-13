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

public record PacketUpdateWindowLong(short windowId, short propId, long propVal) implements IPacket {
    public static final CustomPacketPayload.Type<PacketUpdateWindowLong> TYPE = new CustomPacketPayload.Type<>(Main.rl("update_window_long"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateWindowLong> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.SHORT, PacketUpdateWindowLong::windowId,
        ByteBufCodecs.SHORT, PacketUpdateWindowLong::propId,
        ByteBufCodecs.VAR_LONG, PacketUpdateWindowLong::propVal,
        PacketUpdateWindowLong::new
    );

    @Override
    public void handle(IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarLong(propId, propVal);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}