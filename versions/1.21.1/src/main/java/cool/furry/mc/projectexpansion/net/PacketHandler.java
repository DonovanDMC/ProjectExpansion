package cool.furry.mc.projectexpansion.net;

import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.net.packets.to_client.*;
import cool.furry.mc.projectexpansion.net.packets.to_server.*;
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
        registrar.playToServer(PacketArcaneTransmutationTabletRecipeTransfer.TYPE, PacketArcaneTransmutationTabletRecipeTransfer.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketArcaneTransmutationTabletSelectRecipe.TYPE, PacketArcaneTransmutationTabletSelectRecipe.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketArcaneTransmutationTabletSmallButton.TYPE, PacketArcaneTransmutationTabletSmallButton.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketCreateTeleportLocation.TYPE, PacketCreateTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketDeleteTeleportLocation.TYPE, PacketDeleteTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketOpenTransmutationTablet.TYPE, PacketOpenTransmutationTablet.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportBack.TYPE, PacketTeleportBack.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportToLocation.TYPE, PacketTeleportToLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketToggleEmcLock.TYPE, PacketToggleEmcLock.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(ClearKnowledgePacket.TYPE, ClearKnowledgePacket.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketOpenAlchemicalBookGUI.TYPE, PacketOpenAlchemicalBookGUI.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketSyncAlchemicalBookLocations.TYPE, PacketSyncAlchemicalBookLocations.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateCondenserLock.TYPE, PacketUpdateCondenserLock.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowLong.TYPE, PacketUpdateWindowLong.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowInt.TYPE, PacketUpdateWindowInt.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowBigInteger.TYPE, PacketUpdateWindowBigInteger.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(UpdateTransmutationTargetsPacket.TYPE, UpdateTransmutationTargetsPacket.STREAM_CODEC, IPacket::handle);
    }

}
