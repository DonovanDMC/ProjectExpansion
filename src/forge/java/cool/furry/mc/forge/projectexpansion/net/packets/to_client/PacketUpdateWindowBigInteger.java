package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

public record PacketUpdateWindowBigInteger(short windowId, short propId, BigInteger propVal) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarBigInteger(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeShort(windowId);
        buf.writeShort(propId);
        buf.writeUtf(propVal.toString());
    }

    public static PacketUpdateWindowBigInteger decode(FriendlyByteBuf buf) {
        return new PacketUpdateWindowBigInteger(buf.readShort(), buf.readShort(), new BigInteger(buf.readUtf()));
    }
}
