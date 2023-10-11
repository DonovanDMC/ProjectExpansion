package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.ClientSideHandler;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record PacketSyncAlchemicalBookLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleSyncAlchemicalBookLocations(this);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        writeLocationsToBuffer(buf, locations);
        buf.writeBoolean(canEdit);
    }

    public static PacketSyncAlchemicalBookLocations decode(FriendlyByteBuf buf) {
        return new PacketSyncAlchemicalBookLocations(readLocationsFromBuffer(buf), buf.readBoolean());
    }

    public static void writeLocationsToBuffer(FriendlyByteBuf buf, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations) {
        buf.writeVarInt(locations.size());
        locations.forEach(location -> {
            buf.writeNbt(location.serialize());
        });
    }

    public static List<CapabilityAlchemicalBookLocations.TeleportLocation> readLocationsFromBuffer(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        ArrayList<CapabilityAlchemicalBookLocations.TeleportLocation> locations = new ArrayList<>();
        try {
            for (int i = 0; i < size; i++) {
                CapabilityAlchemicalBookLocations.TeleportLocation location = CapabilityAlchemicalBookLocations.TeleportLocation.deserialize(Objects.requireNonNull(buf.readNbt()));
                locations.add(location);
            }
            locations.sort(Comparator.comparingInt(CapabilityAlchemicalBookLocations.TeleportLocation::index));
        } catch (Exception e) {
            LogManager.getLogger(PacketSyncAlchemicalBookLocations.class).error("Failed to read locations from buffer", e);
            Objects.requireNonNull(Minecraft.getInstance().player).sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_CORRUPTED.translateColored(ChatFormatting.RED));
        }
        return locations;
    }
}
