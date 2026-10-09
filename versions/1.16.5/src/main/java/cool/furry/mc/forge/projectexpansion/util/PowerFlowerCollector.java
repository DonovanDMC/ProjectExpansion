package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PowerFlowerCollector {
    private static final Map<UUID, BigInteger> stored = new HashMap<>();
    private static int tick = 0;
    public static void add(ServerPlayerEntity player, BigInteger amount) {
        UUID uuid = player.getUUID();
        stored.put(uuid, stored.containsKey(uuid) ? stored.get(uuid).add(amount) : amount);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        if (tick >= 20) {
            tick = 0;
            Set<UUID> toRemove = new HashSet<>();
            for(Map.Entry<UUID, BigInteger> entry : stored.entrySet()) {
                ServerPlayerEntity player = Util.getPlayer(entry.getKey());
                if (player == null) continue;
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(entry.getKey());
                if(provider == null) continue;
                provider.setEmc(provider.getEmc().add(entry.getValue()));
                provider.syncEmc(player);
                toRemove.add(entry.getKey());
            }
            toRemove.forEach(stored::remove);
        }
    }
}
