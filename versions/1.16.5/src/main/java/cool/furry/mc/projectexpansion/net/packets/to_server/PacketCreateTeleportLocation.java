package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketCreateTeleportLocation implements IPacket {
    private final String name;
    private final Hand hand;

    public PacketCreateTeleportLocation(String name, Hand hand) {
        this.name = name;
        this.hand = hand;
    }

    public String name() {
        return name;
    }

    public Hand hand() {
        return hand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketCreateTeleportLocation that = (PacketCreateTeleportLocation) o;
        return Objects.equals(name, that.name) && Objects.equals(hand, that.hand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, hand);
    }

    @Override
    public String toString() {
        return "PacketCreateTeleportLocation[name=" + name + ", hand=" + hand + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.ensureEditable(player);
                provider.addLocation(player, name);
                provider.sync(player);
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_CREATE_FAILED.translateColored(TextFormatting.RED, error.getComponent()), false);
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeUtf(name);
        buf.writeEnum(hand);
    }

    public static PacketCreateTeleportLocation decode(PacketBuffer buf) {
        return new PacketCreateTeleportLocation(buf.readUtf(), buf.readEnum(Hand.class));
    }
}
