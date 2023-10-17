package cool.furry.mc.forge.projectexpansion.net.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// based on ProjectE's IPEPacket
// https://github.com/sinkillerj/ProjectE/blob/d90e367b058ade5631b709e5feb7d3eacedb6350/src/main/java/moze_intel/projecte/network/packets/IPEPacket.java
public interface IPacket {

    void handle(NetworkEvent.Context context);

    void encode(FriendlyByteBuf buffer);

    static <P extends IPacket> void handle(final P message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> message.handle(context));
        context.setPacketHandled(true);
    }
}
