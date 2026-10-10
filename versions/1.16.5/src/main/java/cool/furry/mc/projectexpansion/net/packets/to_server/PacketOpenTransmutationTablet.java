package cool.furry.mc.projectexpansion.net.packets.to_server;

import cool.furry.mc.projectexpansion.integrations.curios.CuriosIntegration;
import cool.furry.mc.projectexpansion.net.packets.IPacket;
import cool.furry.mc.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Optional;

public class PacketOpenTransmutationTablet implements IPacket {
    public static final PacketOpenTransmutationTablet INSTANCE = new PacketOpenTransmutationTablet();

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) return;
        Optional<IItemHandlerModifiable> curiosInv = CuriosIntegration.getCuriosInventory(player);
        if (!curiosInv.isPresent()) return;
        IItemHandlerModifiable curios = curiosInv.get();
        for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            Item item = stack.getItem();
            if (item instanceof ITransmutationTablet) {
                ITransmutationTablet tablet = ((ITransmutationTablet) item);
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
    public void encode(PacketBuffer buf) {
    }

    public static PacketOpenTransmutationTablet decode(PacketBuffer buf) {
        return INSTANCE;
    }
}
