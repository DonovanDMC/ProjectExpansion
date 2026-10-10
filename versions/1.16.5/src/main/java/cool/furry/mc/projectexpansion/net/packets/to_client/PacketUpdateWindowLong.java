package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketUpdateWindowLong implements IPacket {
    private final short windowId;
    private final short propId;
    private final long propVal;

    public PacketUpdateWindowLong(short windowId, short propId, long propVal) {
        this.windowId = windowId;
        this.propId = propId;
        this.propVal = propVal;
    }

    public short windowId() {
        return windowId;
    }

    public short propId() {
        return propId;
    }

    public long propVal() {
        return propVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketUpdateWindowLong that = (PacketUpdateWindowLong) o;
        return Objects.equals(windowId, that.windowId) && Objects.equals(propId, that.propId) && Objects.equals(propVal, that.propVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, propId, propVal);
    }

    @Override
    public String toString() {
        return "PacketUpdateWindowLong[windowId=" + windowId + ", propId=" + propId + ", propVal=" + propVal + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase && player.containerMenu.containerId == windowId) {
            ContainerBase container = ((ContainerBase) player.containerMenu);
            container.updateProgressBarLong(propId, propVal);
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeShort(windowId);
        buf.writeShort(propId);
        buf.writeVarLong(propVal);
    }

    public static PacketUpdateWindowLong decode(PacketBuffer buf) {
        return new PacketUpdateWindowLong(buf.readShort(), buf.readShort(), buf.readVarLong());
    }
}
