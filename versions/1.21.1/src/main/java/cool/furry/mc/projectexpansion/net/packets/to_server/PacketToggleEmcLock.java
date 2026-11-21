package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// slot is in the player's inventory, not the open menu, so it's the same from every screen, including creative's
public record PacketToggleEmcLock(int slot) implements IPacket {
    public static final CustomPacketPayload.Type<PacketToggleEmcLock> TYPE = new CustomPacketPayload.Type<>(Main.rl("toggle_emc_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleEmcLock> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacketToggleEmcLock::slot,
            PacketToggleEmcLock::new
    );

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) return;
        ItemStack stack = inventory.getItem(slot);
        if (!EmcLock.canToggle(stack)) return;
        EmcLock.toggle(stack);
        inventory.setChanged();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
