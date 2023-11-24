package cool.furry.mc.forge.projectexpansion.net.packets.to_server;


import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class PacketDeleteTeleportDestination implements IPacket {
    final String name;
    final Player player;
    final InteractionHand hand;
    public PacketDeleteTeleportDestination(String name, Player player, InteractionHand hand) {
        this.name = name;
        this.player = player;
        this.hand = hand;
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.ensureEditable((ServerPlayer) player);
                provider.removeLocation(name);
                provider.sync((ServerPlayer) player);
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_DELETE_FAILED.translateColored(ChatFormatting.RED, error.getComponent()));
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketDeleteTeleportDestination decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        UUID uuid = buf.readUUID();
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in DeleteTeleportDestination packet");
        }
        return new PacketDeleteTeleportDestination(name, player, hand);
    }
}
