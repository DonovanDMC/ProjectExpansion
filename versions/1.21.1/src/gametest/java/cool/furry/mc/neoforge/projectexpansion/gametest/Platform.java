package cool.furry.mc.neoforge.projectexpansion.gametest;

import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

// kept in line with the Forge versions' Platform, so the tests stay the same
public class Platform {
    public static long emcValue(Item item) {
        return IEMCProxy.INSTANCE.getValue(item);
    }

    // ProjectE maps EMC when datapacks are synced, which normally happens when the first player joins
    public static void prepareEmc(MinecraftServer server) {
        NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(server.getPlayerList(), null));
    }

    public static boolean hasAdvancement(MinecraftServer server, ResourceLocation id) {
        return server.getAdvancements().get(id) != null;
    }
}
