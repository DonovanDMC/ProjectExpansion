package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.BufferHelper;
import cool.furry.mc.projectexpansion.util.ClientSideHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;

public final class PacketSyncAlchemicalBookLocations implements IPacket {
    private final List<CapabilityAlchemicalBookLocations.TeleportLocation> locations;
    private final boolean canEdit;

    public PacketSyncAlchemicalBookLocations(List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, boolean canEdit) {
        this.locations = locations;
        this.canEdit = canEdit;
    }

    public List<CapabilityAlchemicalBookLocations.TeleportLocation> locations() {
        return locations;
    }

    public boolean canEdit() {
        return canEdit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketSyncAlchemicalBookLocations that = (PacketSyncAlchemicalBookLocations) o;
        return Objects.equals(locations, that.locations) && Objects.equals(canEdit, that.canEdit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations, canEdit);
    }

    @Override
    public String toString() {
        return "PacketSyncAlchemicalBookLocations[locations=" + locations + ", canEdit=" + canEdit + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleSyncAlchemicalBookLocations(this);
    }

    @Override
    public void encode(PacketBuffer buf) {
        BufferHelper.writeCollection(buf, locations, (b, location) -> location.write(b));
        buf.writeBoolean(canEdit);
    }

    public static PacketSyncAlchemicalBookLocations decode(PacketBuffer buf) {
        return new PacketSyncAlchemicalBookLocations(BufferHelper.readList(buf, CapabilityAlchemicalBookLocations.TeleportLocation::read), buf.readBoolean());
    }
}
