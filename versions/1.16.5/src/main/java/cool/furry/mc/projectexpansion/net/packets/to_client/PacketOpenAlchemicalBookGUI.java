package cool.furry.mc.projectexpansion.net.packets.to_client;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.BufferHelper;
import cool.furry.mc.projectexpansion.util.ClientSideHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;

public final class PacketOpenAlchemicalBookGUI implements IPacket {
    private final Hand hand;
    private final List<CapabilityAlchemicalBookLocations.TeleportLocation> locations;
    private final ItemAlchemicalBook.Mode mode;
    private final boolean canEdit;

    public PacketOpenAlchemicalBookGUI(Hand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, ItemAlchemicalBook.Mode mode, boolean canEdit) {
        this.hand = hand;
        this.locations = locations;
        this.mode = mode;
        this.canEdit = canEdit;
    }

    public Hand hand() {
        return hand;
    }

    public List<CapabilityAlchemicalBookLocations.TeleportLocation> locations() {
        return locations;
    }

    public ItemAlchemicalBook.Mode mode() {
        return mode;
    }

    public boolean canEdit() {
        return canEdit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketOpenAlchemicalBookGUI that = (PacketOpenAlchemicalBookGUI) o;
        return Objects.equals(hand, that.hand) && Objects.equals(locations, that.locations) && Objects.equals(mode, that.mode) && Objects.equals(canEdit, that.canEdit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hand, locations, mode, canEdit);
    }

    @Override
    public String toString() {
        return "PacketOpenAlchemicalBookGUI[hand=" + hand + ", locations=" + locations + ", mode=" + mode + ", canEdit=" + canEdit + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleAlchemicalBookOpen(this);
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(hand);
        BufferHelper.writeCollection(buf, locations, (b, location) -> location.write(b));
        buf.writeEnum(mode);
        buf.writeBoolean(canEdit);
    }

    public static PacketOpenAlchemicalBookGUI decode(PacketBuffer buf) {
        return new PacketOpenAlchemicalBookGUI(buf.readEnum(Hand.class), BufferHelper.readList(buf, CapabilityAlchemicalBookLocations.TeleportLocation::read), buf.readEnum(ItemAlchemicalBook.Mode.class), buf.readBoolean());
    }
}
