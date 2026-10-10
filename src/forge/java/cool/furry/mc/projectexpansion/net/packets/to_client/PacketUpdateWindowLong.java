package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PacketUpdateWindowLong(short windowId, short propId, long propVal) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarLong(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeShort(windowId);
        buf.writeShort(propId);
        buf.writeVarLong(propVal);
    }

    public static PacketUpdateWindowLong decode(FriendlyByteBuf buf) {
        return new PacketUpdateWindowLong(buf.readShort(), buf.readShort(), buf.readVarLong());
    }
}
