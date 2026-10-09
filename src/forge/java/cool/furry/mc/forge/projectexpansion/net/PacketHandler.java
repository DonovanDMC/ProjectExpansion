package cool.furry.mc.forge.projectexpansion.net;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.net.packets.to_client.*;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

// based on ProjectE's PacketHandler
// https://github.com/sinkillerj/ProjectE/blob/68fbb2dea0cf8a6394fa6c7c084063046d94cee5/src/main/java/moze_intel/projecte/network/PacketHandler.java
public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(Main.rl("main"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();
    private static int index = 0;

    public static void register() {
        registerToServer(PacketArcaneTransmutationTabletRecipeTransfer.class, PacketArcaneTransmutationTabletRecipeTransfer::decode);
        registerToServer(PacketArcaneTransmutationTabletSelectRecipe.class, PacketArcaneTransmutationTabletSelectRecipe::decode);
        registerToServer(PacketArcaneTransmutationTabletSmallButton.class, PacketArcaneTransmutationTabletSmallButton::decode);
        registerToServer(PacketCreateTeleportLocation.class, PacketCreateTeleportLocation::decode);
        registerToServer(PacketDeleteTeleportLocation.class, PacketDeleteTeleportLocation::decode);
        registerToServer(PacketOpenTransmutationTablet.class, PacketOpenTransmutationTablet::decode);
        registerToServer(PacketTeleportBack.class, PacketTeleportBack::decode);
        registerToServer(PacketTeleportToLocation.class, PacketTeleportToLocation::decode);
        registerToServer(PacketToggleEmcLock.class, PacketToggleEmcLock::decode);
        registerToClient(PacketOpenAlchemicalBookGUI.class, PacketOpenAlchemicalBookGUI::decode);
        registerToClient(PacketSyncAlchemicalBookLocations.class, PacketSyncAlchemicalBookLocations::decode);
        registerToClient(PacketUpdateCondenserLock.class, PacketUpdateCondenserLock::decode);
        registerToClient(PacketUpdateWindowLong.class, PacketUpdateWindowLong::decode);
        registerToClient(PacketUpdateWindowInt.class, PacketUpdateWindowInt::decode);
        registerToClient(PacketUpdateWindowBigInteger.class, PacketUpdateWindowBigInteger::decode);
    }

    private static <P extends IPacket> void registerToServer(Class<P> type, Function<FriendlyByteBuf, P> decoder) {
        register(type, decoder, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <P extends IPacket> void registerToClient(Class<P> type, Function<FriendlyByteBuf, P> decoder) {
        register(type, decoder, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <P extends IPacket> void register(Class<P> type, Function<FriendlyByteBuf, P> decoder, NetworkDirection direction) {
        CHANNEL.registerMessage(index++, type, IPacket::encode, decoder, PacketHandler::handle, Optional.of(direction));
    }

    private static <P extends IPacket> void handle(P packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> packet.handle(context));
        context.setPacketHandled(true);
    }

    public static void sendToServer(IPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, IPacket packet) {
        if (!(player instanceof FakePlayer)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
