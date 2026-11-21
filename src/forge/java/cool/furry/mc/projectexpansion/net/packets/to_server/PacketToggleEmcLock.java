package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

// slot is in the player's inventory, not the open menu, so it's the same from every screen, including creative's
public record PacketToggleEmcLock(int slot) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) return;
        ItemStack stack = inventory.getItem(slot);
        if (!EmcLock.canToggle(stack)) return;
        EmcLock.toggle(stack);
        inventory.setChanged();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
    }

    public static PacketToggleEmcLock decode(FriendlyByteBuf buf) {
        return new PacketToggleEmcLock(buf.readVarInt());
    }
}
