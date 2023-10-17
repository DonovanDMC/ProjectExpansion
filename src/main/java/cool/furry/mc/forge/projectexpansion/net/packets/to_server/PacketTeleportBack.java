package cool.furry.mc.forge.projectexpansion.net.packets.to_server;


import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;

public class PacketTeleportBack implements IPacket {
    final PlayerEntity player;
    final Hand hand;
    public PacketTeleportBack(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.teleportBack((ServerPlayerEntity) player, book.getTier().isAcrossDimensions());
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                Util.sendSystemMessage(player, Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(TextFormatting.RED, error.getComponent()));
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketTeleportBack decode(PacketBuffer buf) {
        UUID uuid = buf.readUUID();
        Hand hand = buf.readEnum(Hand.class);
        ServerPlayerEntity player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in PacketTeleportBack packet");
        }
        return new PacketTeleportBack(player, hand);
    }
}
