package cool.furry.mc.forge.projectexpansion.net.packets.to_server;


import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.forge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.math.BigInteger;
import java.util.UUID;

public class PacketTeleportToDestination implements IPacket {
    final String name;
    final PlayerEntity player;
    final Hand hand;
    public PacketTeleportToDestination(String name, PlayerEntity player, Hand hand) {
        this.name = name;
        this.player = player;
        this.hand = hand;
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            ItemAlchemicalBook book = (ItemAlchemicalBook) stack.getItem();
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                IKnowledgeProvider knowledgeProvider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY)
                        .orElseThrow(() -> new IllegalStateException("Player does not have knowledge capability"));
                BigInteger emc = knowledgeProvider.getEmc();
                CapabilityAlchemicalBookLocations.TeleportLocation location = provider.getLocationOrThrow(name);
                BigInteger cost = BigInteger.valueOf(location.getCost(stack, player));
                if(!cost.equals(BigInteger.ZERO)) {
                    if (emc.compareTo(cost) < 0) {
                        throw new CapabilityAlchemicalBookLocations.BookError.NotEnoughEMCError(cost.toString());
                    }
                    knowledgeProvider.setEmc(emc.subtract(cost));
                    knowledgeProvider.syncEmc((ServerPlayerEntity) player);
                }
                GlobalPos pos = GlobalPos.of(player.level.dimension(), player.getOnPos());
                location.teleportTo((ServerPlayerEntity) player, book.getTier().isAcrossDimensions());
                if(location.distanceFrom(pos.pos()) > 1) {
                    provider.saveBackLocation(player, pos);
                }
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                Util.sendSystemMessage(player, Lang.Items.ALCHEMICAL_BOOK_TELEPORT_FAILED.translateColored(TextFormatting.RED, error.getComponent()));
            } catch (IllegalStateException error) {
                Util.sendSystemMessage(player, Lang.PROVIDER_ERROR.translateColored(TextFormatting.RED));
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeUtf(name);
        buf.writeUUID(player.getUUID());
        buf.writeEnum(hand);
    }

    public static PacketTeleportToDestination decode(PacketBuffer buf) {
        String name = buf.readUtf();
        UUID uuid = buf.readUUID();
        Hand hand = buf.readEnum(Hand.class);
        ServerPlayerEntity player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            throw new IllegalStateException("Player not found in PacketTeleportToDestination packet");
        }
        return new PacketTeleportToDestination(name, player, hand);
    }
}
