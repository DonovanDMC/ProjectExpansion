package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.ClientSideHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;

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

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleAlchemicalBookOpen(this);
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(hand);
        PacketSyncAlchemicalBookLocations.writeLocationsToBuffer(buf, locations);
        buf.writeEnum(mode);
        buf.writeBoolean(canEdit);
    }

    public static PacketOpenAlchemicalBookGUI decode(PacketBuffer buf) {
        return new PacketOpenAlchemicalBookGUI(buf.readEnum(Hand.class), PacketSyncAlchemicalBookLocations.readLocationsFromBuffer(buf), buf.readEnum(ItemAlchemicalBook.Mode.class), buf.readBoolean());
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
}
