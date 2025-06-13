package cool.furry.mc.neoforge.projectexpansion.net.packets;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// based on ProjectE's IPEPacket
// https://github.com/sinkillerj/ProjectE/blob/ef07e3092b7907c48f3c080240bfa62ad456f357/src/main/java/moze_intel/projecte/network/packets/IPEPacket.java
public interface IPacket extends CustomPacketPayload {
    void handle(IPayloadContext context);
}
