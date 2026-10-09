package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.EmcLock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record PacketToggleEmcLock(int containerId, int slot) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId || slot < 0 || slot >= menu.slots.size()) return;
        // only allow toggling items in the player's own inventory
        Slot target = menu.getSlot(slot);
        if (target.container != player.getInventory()) return;
        ItemStack stack = target.getItem();
        if (!EmcLock.canToggle(stack)) return;
        EmcLock.toggle(stack);
        target.setChanged();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slot);
    }

    public static PacketToggleEmcLock decode(FriendlyByteBuf buf) {
        return new PacketToggleEmcLock(buf.readVarInt(), buf.readVarInt());
    }
}
