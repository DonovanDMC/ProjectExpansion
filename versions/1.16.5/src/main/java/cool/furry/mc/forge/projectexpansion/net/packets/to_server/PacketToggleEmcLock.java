package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.EmcLock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketToggleEmcLock implements IPacket {
    private final int containerId;
    private final int slot;

    public PacketToggleEmcLock(int containerId, int slot) {
        this.containerId = containerId;
        this.slot = slot;
    }

    public int containerId() {
        return containerId;
    }

    public int slot() {
        return slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketToggleEmcLock that = (PacketToggleEmcLock) o;
        return Objects.equals(containerId, that.containerId) && Objects.equals(slot, that.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerId, slot);
    }

    @Override
    public String toString() {
        return "PacketToggleEmcLock[containerId=" + containerId + ", slot=" + slot + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) return;
        Container menu = player.containerMenu;
        if (menu.containerId != containerId || slot < 0 || slot >= menu.slots.size()) return;
        // only allow toggling items in the player's own inventory
        Slot target = menu.getSlot(slot);
        if (target.container != player.inventory) return;
        ItemStack stack = target.getItem();
        if (!EmcLock.canToggle(stack)) return;
        EmcLock.toggle(stack);
        target.setChanged();
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slot);
    }

    public static PacketToggleEmcLock decode(PacketBuffer buf) {
        return new PacketToggleEmcLock(buf.readVarInt(), buf.readVarInt());
    }
}
