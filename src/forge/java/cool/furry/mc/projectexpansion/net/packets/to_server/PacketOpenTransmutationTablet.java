package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.integrations.curios.CuriosIntegration;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;

public class PacketOpenTransmutationTablet implements IPacket {
    public static final PacketOpenTransmutationTablet INSTANCE = new PacketOpenTransmutationTablet();

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
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
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    public static PacketOpenTransmutationTablet decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }
}
