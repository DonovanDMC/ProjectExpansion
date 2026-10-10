package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.Lang;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

import java.math.BigInteger;
import java.util.Objects;

public final class PacketTeleportToLocation implements IPacket {
    private final String name;
    private final Hand hand;

    public PacketTeleportToLocation(String name, Hand hand) {
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
        PacketTeleportToLocation that = (PacketTeleportToLocation) o;
        return Objects.equals(name, that.name) && Objects.equals(hand, that.hand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, hand);
    }

    @Override
    public String toString() {
        return "PacketTeleportToLocation[name=" + name + ", hand=" + hand + "]";
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
                IKnowledgeProvider knowledgeProvider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).orElseThrow(() -> new IllegalStateException("Player does not have knowledge capability"));
                BigInteger emc = knowledgeProvider.getEmc();
                CapabilityAlchemicalBookLocations.TeleportLocation location = provider.getLocationOrThrow(name);
                BigInteger cost = BigInteger.valueOf(location.getCost(stack, player));
                if(!cost.equals(BigInteger.ZERO)) {
                    if (emc.compareTo(cost) < 0) {
                        throw new CapabilityAlchemicalBookLocations.BookError.NotEnoughEMCError(cost.toString());
                    }
                    knowledgeProvider.setEmc(emc.subtract(cost));
                    knowledgeProvider.syncEmc(player);
                }
                GlobalPos pos = GlobalPos.of(player.level.dimension(), player.blockPosition());
                location.teleportTo(player, book.getTier().isAcrossDimensions());
                if(location.distanceFrom(pos.pos()) > 1) {
                    provider.saveBackLocation(player, pos);
                }
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(TextFormatting.RED, error.getComponent()), false);
            } catch (IllegalStateException error) {
                player.displayClientMessage(Lang.PROVIDER_ERROR.translateColored(TextFormatting.RED), false);
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeUtf(name);
        buf.writeEnum(hand);
    }

    public static PacketTeleportToLocation decode(PacketBuffer buf) {
        return new PacketTeleportToLocation(buf.readUtf(), buf.readEnum(Hand.class));
    }
}
