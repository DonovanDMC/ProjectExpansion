package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.ClientSideHandler;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PacketSyncAlchemicalBookLocations implements IPacket {
    private final List<CapabilityAlchemicalBookLocations.TeleportLocation> locations;
    private final boolean canEdit;

    public PacketSyncAlchemicalBookLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        this.locations = locations;
        this.canEdit = canEdit;
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleSyncAlchemicalBookLocations(this);
    }

    @Override
    public void encode(PacketBuffer buf) {
        writeLocationsToBuffer(buf, locations);
        buf.writeBoolean(canEdit);
    }

    public static PacketSyncAlchemicalBookLocations decode(PacketBuffer buf) {
        return new PacketSyncAlchemicalBookLocations(readLocationsFromBuffer(buf), buf.readBoolean());
    }

    public static void writeLocationsToBuffer(PacketBuffer buf, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations) {
        buf.writeInt(locations.size());
        locations.forEach(location -> buf.writeNbt(location.serialize()));
    }

    public static List<CapabilityAlchemicalBookLocations.TeleportLocation> readLocationsFromBuffer(PacketBuffer buf) {
        int size = buf.readInt();
        ArrayList<CapabilityAlchemicalBookLocations.TeleportLocation> locations = new ArrayList<>();
        try {
            for (int i = 0; i < size; i++) {
                CapabilityAlchemicalBookLocations.TeleportLocation location = CapabilityAlchemicalBookLocations.TeleportLocation.deserialize(Objects.requireNonNull(buf.readNbt()));
                locations.add(location);
            }
            locations.sort(Comparator.comparingInt(CapabilityAlchemicalBookLocations.TeleportLocation::index));
        } catch (Exception e) {
            LogManager.getLogger(PacketSyncAlchemicalBookLocations.class).error("Failed to read locations from buffer", e);
            Util.sendSystemMessage(Objects.requireNonNull(Minecraft.getInstance().player), Lang.Items.ALCHEMICAL_BOOK_CORRUPTED.translateColored(TextFormatting.RED));
        }
        return locations;
    }

    public List<CapabilityAlchemicalBookLocations.TeleportLocation> locations() {
        return locations;
    }

    public boolean canEdit() {
        return canEdit;
    }

}
