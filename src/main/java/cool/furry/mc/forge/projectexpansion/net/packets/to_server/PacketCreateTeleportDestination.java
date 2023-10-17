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

public class PacketCreateTeleportDestination implements IPacket {
    final String name;
    final PlayerEntity player;
    final Hand hand;
    public PacketCreateTeleportDestination(String name, PlayerEntity player, Hand hand) {
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
                provider.ensureEditable((ServerPlayerEntity) player);
                provider.addLocation(player, name);
                provider.sync((ServerPlayerEntity) player);
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                Util.sendSystemMessage(player, Lang.Items.ALCHEMICAL_BOOK_CREATE_FAILED.translateColored(TextFormatting.RED, error.getComponent()));
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeUtf(name);
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketCreateTeleportDestination decode(PacketBuffer buf) {
        String name = buf.readUtf();
        UUID uuid = buf.readUUID();
        Hand hand = buf.readEnum(Hand.class);
        ServerPlayerEntity player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in PacketCreateTeleportDestination packet");
        }
        return new PacketCreateTeleportDestination(name, player, hand);
    }
}
