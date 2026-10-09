package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.ClientSideHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

public record PacketSyncAlchemicalBookLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleSyncAlchemicalBookLocations(this);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(locations, (b, location) -> location.write(b));
        buf.writeBoolean(canEdit);
    }

    public static PacketSyncAlchemicalBookLocations decode(FriendlyByteBuf buf) {
        return new PacketSyncAlchemicalBookLocations(buf.readList(CapabilityAlchemicalBookLocations.TeleportLocation::read), buf.readBoolean());
    }
}
