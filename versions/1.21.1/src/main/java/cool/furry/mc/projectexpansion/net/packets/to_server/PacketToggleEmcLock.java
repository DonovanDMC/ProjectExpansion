package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.EmcLock;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketToggleEmcLock(int containerId, int slot) implements IPacket {
    public static final CustomPacketPayload.Type<PacketToggleEmcLock> TYPE = new CustomPacketPayload.Type<>(Main.rl("toggle_emc_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleEmcLock> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacketToggleEmcLock::containerId,
            ByteBufCodecs.VAR_INT, PacketToggleEmcLock::slot,
            PacketToggleEmcLock::new
    );

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
