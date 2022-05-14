package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.math.BigInteger;
import java.util.*;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PowerFlowerCollector {
    private static final Map<UUID, BigInteger> stored = new HashMap<>();
    private static int tick = 0;

    public static void add(ServerPlayerEntity player, BigInteger amount) {
        UUID uuid = player.getUniqueID();
        stored.put(uuid, stored.containsKey(uuid) ? stored.get(uuid).add(amount) : amount);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        tick++;
        if (tick >= 20) {
            tick = 0;
            Set<UUID> toRemove = new HashSet<>();
            for(UUID uuid : stored.keySet()) {
                BigInteger amount = stored.get(uuid);
                ServerPlayerEntity player = Util.getPlayer(uuid);
                if (player == null) continue;
                IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(uuid);
                provider.setEmc(provider.getEmc().add(amount));
                provider.syncEmc(player);
                toRemove.add(uuid);
            }
            toRemove.forEach(stored::remove);
        }
    }
}
