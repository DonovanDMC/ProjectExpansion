package cool.furry.mc.forge.projectexpansion.net.packets;

import cool.furry.mc.forge.projectexpansion.Main;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;

import java.util.UUID;

public class EMCSyncRequest implements IPacket {
    private final UUID player;

    public EMCSyncRequest(UUID player) {
        this.player = player;
    }

    public static EMCSyncRequest decode(PacketBuffer buf) {
        UUID uuid = buf.readUniqueId();
        return new EMCSyncRequest(uuid);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayerEntity playerEntity = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(player);
        if (playerEntity == null) {
            Main.Logger.printf(Level.INFO, "EMC sync request failed for player %s, getPlayerByUUID returned null.", player.toString());
            return;
        }
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player);
        provider.syncEmc(playerEntity);
    }

    @Override
    public void encode(PacketBuffer buffer) {
        buffer.writeUniqueId(player);
    }
}
