package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PacketUpdateWindowInt(short windowId, short propId, int propVal) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarInt(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeShort(windowId);
        buf.writeShort(propId);
        buf.writeInt(propVal);
    }

    public static PacketUpdateWindowInt decode(FriendlyByteBuf buf) {
        return new PacketUpdateWindowInt(buf.readShort(), buf.readShort(), buf.readInt());
    }
}
