package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

public record PacketTeleportToLocation(String name, InteractionHand hand) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook book) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                IKnowledgeProvider knowledgeProvider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(() -> new IllegalStateException("Player does not have knowledge capability"));
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
                GlobalPos pos = GlobalPos.of(Platform.level(player).dimension(), player.blockPosition());
                location.teleportTo(player, book.getTier().isAcrossDimensions());
                if(!location.dimension().equals(pos.dimension()) || location.distanceFrom(pos.pos()) > 1) {
                    provider.saveBackLocation(player, pos);
                }
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.displayClientMessage(Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(ChatFormatting.RED, error.getComponent()), false);
            } catch (IllegalStateException error) {
                player.displayClientMessage(Lang.PROVIDER_ERROR.translateColored(ChatFormatting.RED), false);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeEnum(hand);
    }

    public static PacketTeleportToLocation decode(FriendlyByteBuf buf) {
        return new PacketTeleportToLocation(buf.readUtf(), buf.readEnum(InteractionHand.class));
    }
}
