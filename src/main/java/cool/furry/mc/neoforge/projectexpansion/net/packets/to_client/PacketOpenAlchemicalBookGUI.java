package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.util.ClientSideHandler;
import moze_intel.projecte.network.PEStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PacketOpenAlchemicalBookGUI(InteractionHand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, ItemAlchemicalBook.Mode mode, boolean canEdit) implements IPacket {
    public static final CustomPacketPayload.Type<PacketOpenAlchemicalBookGUI> TYPE = new CustomPacketPayload.Type<>(Main.rl("open_alchemical_book_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenAlchemicalBookGUI> STREAM_CODEC = StreamCodec.composite(
            PEStreamCodecs.INTERACTION_HAND, PacketOpenAlchemicalBookGUI::hand,
            CapabilityAlchemicalBookLocations.TeleportLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), PacketOpenAlchemicalBookGUI::locations,
            ItemAlchemicalBook.Mode.STREAM_CODEC, PacketOpenAlchemicalBookGUI::mode,
            ByteBufCodecs.BOOL, PacketOpenAlchemicalBookGUI::canEdit,
            PacketOpenAlchemicalBookGUI::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        ClientSideHandler.handleAlchemicalBookOpen(this);
    }
}
