package cool.furry.mc.projectexpansion.net.packets;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// based on ProjectE's IPEPacket
// https://github.com/sinkillerj/ProjectE/blob/68fbb2dea0cf8a6394fa6c7c084063046d94cee5/src/main/java/moze_intel/projecte/network/packets/IPEPacket.java
public interface IPacket {
    void handle(NetworkEvent.Context context);

    void encode(PacketBuffer buf);
}
