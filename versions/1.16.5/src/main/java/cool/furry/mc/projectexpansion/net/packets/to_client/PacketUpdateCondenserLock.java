package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Input;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class PacketUpdateCondenserLock implements IPacket {
    private final short windowId;
    @Nullable private final ItemInfo lockInfo;

    public PacketUpdateCondenserLock(short windowId, @Nullable ItemInfo lockInfo) {
        this.windowId = windowId;
        this.lockInfo = lockInfo;
    }

    public short windowId() {
        return windowId;
    }

    @Nullable public ItemInfo lockInfo() {
        return lockInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketUpdateCondenserLock that = (PacketUpdateCondenserLock) o;
        return Objects.equals(windowId, that.windowId) && Objects.equals(lockInfo, that.lockInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, lockInfo);
    }

    @Override
    public String toString() {
        return "PacketUpdateCondenserLock[windowId=" + windowId + ", lockInfo=" + lockInfo + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerCondenserMK3Input && ((ContainerCondenserMK3Input) player.containerMenu).containerId == windowId) {
            ContainerCondenserMK3Input container = ((ContainerCondenserMK3Input) player.containerMenu);
            container.updateLockInfo(lockInfo);
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeShort(windowId);
        if (lockInfo == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, lockInfo.getItem());
            buf.writeNbt(lockInfo.getNBT());
        }
    }

    public static PacketUpdateCondenserLock decode(PacketBuffer buf) {
        short windowId = buf.readShort();
        ItemInfo lockInfo = null;
        if (buf.readBoolean()) {
            lockInfo = ItemInfo.fromItem(buf.readRegistryIdUnsafe(ForgeRegistries.ITEMS), buf.readNbt());
        }
        return new PacketUpdateCondenserLock(windowId, lockInfo);
    }
}
