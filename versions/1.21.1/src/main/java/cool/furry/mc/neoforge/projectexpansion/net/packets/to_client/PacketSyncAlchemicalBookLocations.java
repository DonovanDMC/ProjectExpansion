package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.util.ClientSideHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PacketSyncAlchemicalBookLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) implements IPacket {
    public static final CustomPacketPayload.Type<PacketSyncAlchemicalBookLocations> TYPE = new CustomPacketPayload.Type<>(Main.rl("sync_alchemical_book_locations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncAlchemicalBookLocations> STREAM_CODEC = StreamCodec.composite(
        CapabilityAlchemicalBookLocations.TeleportLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), PacketSyncAlchemicalBookLocations::locations,
        ByteBufCodecs.BOOL, PacketSyncAlchemicalBookLocations::canEdit,
        PacketSyncAlchemicalBookLocations::new
    );

    @Override
    public void handle(IPayloadContext context) {
        ClientSideHandler.handleSyncAlchemicalBookLocations(this);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
