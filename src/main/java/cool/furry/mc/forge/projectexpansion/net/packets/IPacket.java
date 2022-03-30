package cool.furry.mc.forge.projectexpansion.net.packets;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

public interface IPacket {

    static <PACKET extends IPacket> void handle(final PACKET message, Supplier<Context> ctx) {
        Context context = ctx.get();
        context.enqueueWork(() -> message.handle(context));
        context.setPacketHandled(true);
    }

    void handle(NetworkEvent.Context context);

    void encode(PacketBuffer buffer);
}