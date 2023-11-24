package cool.furry.mc.forge.projectexpansion.net.packets.to_server;


import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.math.BigInteger;
import java.util.UUID;

public class PacketTeleportToDestination implements IPacket {
    final String name;
    final Player player;
    final InteractionHand hand;
    public PacketTeleportToDestination(String name, Player player, InteractionHand hand) {
        this.name = name;
        this.player = player;
        this.hand = hand;
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook book) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                IKnowledgeProvider knowledgeProvider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Player does not have knowledge capability"));
                BigInteger emc = knowledgeProvider.getEmc();
                CapabilityAlchemicalBookLocations.TeleportLocation location = provider.getLocationOrThrow(name);
                BigInteger cost = BigInteger.valueOf(location.getCost(stack, player));
                if(!cost.equals(BigInteger.ZERO)) {
                    if (emc.compareTo(cost) < 0) {
                        throw new CapabilityAlchemicalBookLocations.BookError.NotEnoughEMCError(cost.toString());
                    }
                    knowledgeProvider.setEmc(emc.subtract(cost));
                    knowledgeProvider.syncEmc((ServerPlayer) player);
                }
                GlobalPos pos = GlobalPos.of(player.level().dimension(), player.getOnPos());
                location.teleportTo((ServerPlayer) player, book.getTier().isAcrossDimensions());
                if(location.distanceFrom(pos.pos()) > 1) {
                    provider.saveBackLocation(player, pos);
                }
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(ChatFormatting.RED, error.getComponent()));
            } catch (IllegalStateException error) {
                player.sendSystemMessage(Lang.PROVIDER_ERROR.translateColored(ChatFormatting.RED));
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketTeleportToDestination decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        UUID uuid = buf.readUUID();
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in PacketTeleportToDestination packet");
        }
        return new PacketTeleportToDestination(name, player, hand);
    }
}
