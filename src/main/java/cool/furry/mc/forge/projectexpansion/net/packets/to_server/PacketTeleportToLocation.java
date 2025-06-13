package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;


import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.neoforge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.network.PEStreamCodecs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.math.BigInteger;

public record PacketTeleportToLocation(String name, Player player, InteractionHand hand) implements IPacket {
    public static final CustomPacketPayload.Type<PacketTeleportToLocation> TYPE = new CustomPacketPayload.Type<>(Main.rl("teleport_to_location"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketTeleportToLocation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketTeleportToLocation::name,
            Util.PLAYER_STREAM_CODEC, PacketTeleportToLocation::player,
            PEStreamCodecs.INTERACTION_HAND, PacketTeleportToLocation::hand,
            PacketTeleportToLocation::new
    );

    @Override
    public void handle(IPayloadContext context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook book) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                IKnowledgeProvider knowledgeProvider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
                if (knowledgeProvider == null) throw new IllegalStateException("Player does not have knowledge capability");
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
                GlobalPos pos = GlobalPos.of(player.level().dimension(), player.blockPosition());
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
