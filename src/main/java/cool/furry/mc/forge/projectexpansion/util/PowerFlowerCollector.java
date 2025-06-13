package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.Main;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PowerFlowerCollector {
    private static final Map<UUID, BigInteger> stored = new HashMap<>();
    private static int tick = 0;
    public static void add(ServerPlayer player, BigInteger amount) {
        UUID uuid = player.getUUID();
        stored.put(uuid, stored.containsKey(uuid) ? stored.get(uuid).add(amount) : amount);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        tick++;
        if (tick >= 20) {
            tick = 0;
            Set<UUID> toRemove = new HashSet<>();
            for(UUID uuid : stored.keySet()) {
                BigInteger amount = stored.get(uuid);
                ServerPlayer player = Util.getServerPlayer(uuid);
                if (player == null) continue;
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(uuid);
                if(provider == null) continue;
                provider.setEmc(provider.getEmc().add(amount));
                provider.syncEmc(player);
                toRemove.add(uuid);
            }
            toRemove.forEach(stored::remove);
        }
    }
}
