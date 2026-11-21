package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

// slot is in the player's inventory, not the open menu, so it's the same from every screen, including creative's
public final class PacketToggleEmcLock implements IPacket {
    private final int slot;

    public PacketToggleEmcLock(int slot) {
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketToggleEmcLock that = (PacketToggleEmcLock) o;
        return slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot);
    }

    @Override
    public String toString() {
        return "PacketToggleEmcLock[slot=" + slot + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) return;
        PlayerInventory inventory = player.inventory;
        if (slot < 0 || slot >= inventory.getContainerSize()) return;
        ItemStack stack = inventory.getItem(slot);
        if (!EmcLock.canToggle(stack)) return;
        EmcLock.toggle(stack);
        inventory.setChanged();
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeVarInt(slot);
    }

    public static PacketToggleEmcLock decode(PacketBuffer buf) {
        return new PacketToggleEmcLock(buf.readVarInt());
    }
}
