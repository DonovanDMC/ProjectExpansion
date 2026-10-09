package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.math.BigInteger;
import java.util.Objects;

public final class PacketUpdateWindowBigInteger implements IPacket {
    private final short windowId;
    private final short propId;
    private final BigInteger propVal;

    public PacketUpdateWindowBigInteger(short windowId, short propId, BigInteger propVal) {
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

    public BigInteger propVal() {
        return propVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketUpdateWindowBigInteger that = (PacketUpdateWindowBigInteger) o;
        return Objects.equals(windowId, that.windowId) && Objects.equals(propId, that.propId) && Objects.equals(propVal, that.propVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, propId, propVal);
    }

    @Override
    public String toString() {
        return "PacketUpdateWindowBigInteger[windowId=" + windowId + ", propId=" + propId + ", propVal=" + propVal + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase && player.containerMenu.containerId == windowId) {
            ContainerBase container = ((ContainerBase) player.containerMenu);
            container.updateProgressBarBigInteger(propId, propVal);
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeShort(windowId);
        buf.writeShort(propId);
        buf.writeUtf(propVal.toString());
    }

    public static PacketUpdateWindowBigInteger decode(PacketBuffer buf) {
        return new PacketUpdateWindowBigInteger(buf.readShort(), buf.readShort(), new BigInteger(buf.readUtf()));
    }
}
