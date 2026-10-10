package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.gui.container.ContainerCondenserMK3Input;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public record PacketUpdateCondenserLock(short windowId, @Nullable ItemInfo lockInfo) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerCondenserMK3Input container && container.containerId == windowId) {
            container.updateLockInfo(lockInfo);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeShort(windowId);
        if (lockInfo == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, lockInfo.getItem());
            buf.writeNbt(lockInfo.getNBT());
        }
    }

    public static PacketUpdateCondenserLock decode(FriendlyByteBuf buf) {
        short windowId = buf.readShort();
        ItemInfo lockInfo = null;
        if (buf.readBoolean()) {
            lockInfo = ItemInfo.fromItem(buf.readRegistryIdUnsafe(ForgeRegistries.ITEMS), buf.readNbt());
        }
        return new PacketUpdateCondenserLock(windowId, lockInfo);
    }
}
