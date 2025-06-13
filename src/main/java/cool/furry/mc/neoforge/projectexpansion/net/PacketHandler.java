package cool.furry.mc.neoforge.projectexpansion.net;

import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.*;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketCreateTeleportLocation;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketDeleteTeleportLocation;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketTeleportBack;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketTeleportToLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.maven.artifact.versioning.ArtifactVersion;

// based on ProjectE's PacketHandler
// https://github.com/sinkillerj/ProjectE/blob/d90e367b058ade5631b709e5feb7d3eacedb6350/src/main/java/moze_intel/projecte/network/PacketHandler.java
@SuppressWarnings("unused")
public final class PacketHandler {

    public PacketHandler(IEventBus modEventBus, ArtifactVersion version) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(version.toString());
            register(registrar);
        });
    }

    public void register(PayloadRegistrar registrar) {
        registrar.playToServer(PacketCreateTeleportLocation.TYPE, PacketCreateTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketDeleteTeleportLocation.TYPE, PacketDeleteTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportBack.TYPE, PacketTeleportBack.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportToLocation.TYPE, PacketTeleportToLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketOpenAlchemicalBookGUI.TYPE, PacketOpenAlchemicalBookGUI.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketSyncAlchemicalBookLocations.TYPE, PacketSyncAlchemicalBookLocations.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateCondenserLock.TYPE, PacketUpdateCondenserLock.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowLong.TYPE, PacketUpdateWindowLong.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowInt.TYPE, PacketUpdateWindowInt.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowBigInteger.TYPE, PacketUpdateWindowBigInteger.STREAM_CODEC, IPacket::handle);
    }

}
