package cool.furry.mc.forge.projectexpansion.net.packets.to_server;


import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemialBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.jmx.Server;

import javax.annotation.Nullable;
import java.util.UUID;

public class PacketCreateTeleportDestination implements IPacket {
    final String name;
    final Player player;
    final InteractionHand hand;
    public PacketCreateTeleportDestination(String name, Player player, InteractionHand hand) {
        this.name = name;
        this.player = player;
        this.hand = hand;
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            try {
                IAlchemialBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.ensureEditable((ServerPlayer) player);
                provider.addLocation(player, name);
                provider.sync((ServerPlayer) player);
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_CREATE_FAILED.translateColored(ChatFormatting.RED, error.getComponent()));
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketCreateTeleportDestination decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        UUID uuid = buf.readUUID();
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in PacketCreateTeleportDestination packet");
        }
        return new PacketCreateTeleportDestination(name, player, hand);
    }
}
