package cool.furry.mc.forge.projectexpansion.net;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketOpenAlchemicalBookGUI;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketCreateTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketDeleteTeleportDestination;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportBack;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketTeleportToDestination;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Function;

// based on ProjectE's PacketHandler
// https://github.com/sinkillerj/ProjectE/blob/d90e367b058ade5631b709e5feb7d3eacedb6350/src/main/java/moze_intel/projecte/network/PacketHandler.java
@SuppressWarnings("unused")
public class PacketHandler {

    private static final String PROTOCOL_VERSION = Integer.toString(1);
    private static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(Main.rl("primary"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();
    private static int index;

    public static void register() {
        registerClientToServer(PacketCreateTeleportDestination.class, PacketCreateTeleportDestination::decode);
        registerClientToServer(PacketDeleteTeleportDestination.class, PacketDeleteTeleportDestination::decode);
        registerClientToServer(PacketTeleportBack.class, PacketTeleportBack::decode);
        registerClientToServer(PacketTeleportToDestination.class, PacketTeleportToDestination::decode);
        registerServerToClient(PacketOpenAlchemicalBookGUI.class, PacketOpenAlchemicalBookGUI::decode);
        registerServerToClient(PacketSyncAlchemicalBookLocations.class, PacketSyncAlchemicalBookLocations::decode);
    }

    private static <MSG extends IPacket> void registerClientToServer(Class<MSG> type, Function<FriendlyByteBuf, MSG> decoder) {
        registerMessage(type, decoder, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <MSG extends IPacket> void registerServerToClient(Class<MSG> type, Function<FriendlyByteBuf, MSG> decoder) {
        registerMessage(type, decoder, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <MSG extends IPacket> void registerMessage(Class<MSG> type, Function<FriendlyByteBuf, MSG> decoder, NetworkDirection networkDirection) {
        HANDLER.registerMessage(index++, type, IPacket::encode, decoder, IPacket::handle, Optional.of(networkDirection));
    }
    public static <MSG extends IPacket> void sendToServer(MSG msg) {
        HANDLER.sendToServer(msg);
    }
    public static <MSG extends IPacket> void sendTo(MSG msg, ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            HANDLER.sendTo(msg, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }
}
