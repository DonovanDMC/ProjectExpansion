package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.math.BigInteger;

public record PacketUpdateWindowBigInteger(short windowId, short propId, BigInteger propVal) implements IPacket {
    public static final CustomPacketPayload.Type<PacketUpdateWindowBigInteger> TYPE = new CustomPacketPayload.Type<>(Main.rl("update_window_big_integer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateWindowBigInteger> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.SHORT, PacketUpdateWindowBigInteger::windowId,
        ByteBufCodecs.SHORT, PacketUpdateWindowBigInteger::propId,
        Util.BIG_INTEGER_STREAM_CODEC, PacketUpdateWindowBigInteger::propVal,
        PacketUpdateWindowBigInteger::new
    );

    @Override
    public void handle(IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarBigInteger(propId, propVal);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}