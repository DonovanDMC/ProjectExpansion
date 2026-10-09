package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.integrations.curios.CuriosIntegration;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public class PacketOpenTransmutationTablet implements IPacket {
    public static final PacketOpenTransmutationTablet INSTANCE = new PacketOpenTransmutationTablet();
    public static final CustomPacketPayload.Type<PacketOpenTransmutationTablet> TYPE = new CustomPacketPayload.Type<>(Main.rl("open_transmutation_tablet"));
    public static final StreamCodec<FriendlyByteBuf, PacketOpenTransmutationTablet> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer)) return;
            Optional<IItemHandlerModifiable> curiosInv = CuriosIntegration.getCuriosInventory(player);
            if (curiosInv.isEmpty()) return;
            IItemHandlerModifiable curios = curiosInv.get();
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                Item item = stack.getItem();
                if (item instanceof ITransmutationTablet tablet) {
                    tablet.openContainer(player);
                    break;
                // legacy
                } else if (item == PEItems.TRANSMUTATION_TABLET.get()) {
                    Util.openTransmutationTable(player);
                    break;
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
