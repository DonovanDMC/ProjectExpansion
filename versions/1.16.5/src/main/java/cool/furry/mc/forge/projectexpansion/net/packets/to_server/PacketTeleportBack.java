package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

public final class PacketTeleportBack implements IPacket {
    private final Hand hand;

    public PacketTeleportBack(Hand hand) {
        this.hand = hand;
    }

    public Hand hand() {
        return hand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketTeleportBack that = (PacketTeleportBack) o;
        return Objects.equals(hand, that.hand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hand);
    }

    @Override
    public String toString() {
        return "PacketTeleportBack[hand=" + hand + "]";
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.teleportBack(player, book.getTier().isAcrossDimensions());
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(TextFormatting.RED, error.getComponent()), false);
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(hand);
    }

    public static PacketTeleportBack decode(PacketBuffer buf) {
        return new PacketTeleportBack(buf.readEnum(Hand.class));
    }
}
