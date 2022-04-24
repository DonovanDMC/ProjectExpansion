package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if (tick >= (Config.tickDelay.get() + 3)) {
            tick = 0;
            for (UUID uuid : stored.keySet()) {
                BigInteger amount = stored.get(uuid);
                ServerPlayerEntity player = Util.getPlayer(uuid);
                IKnowledgeProvider provider = player == null ? null : player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).orElse(null);
                if (provider == null) continue;
                provider.setEmc(provider.getEmc().add(amount));
                provider.sync(player);
                stored.remove(uuid);
            }
        }
    }
}

