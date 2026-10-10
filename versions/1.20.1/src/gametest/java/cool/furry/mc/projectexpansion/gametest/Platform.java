package cool.furry.mc.projectexpansion.gametest;

import com.mojang.authlib.GameProfile;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

// what the shared GameTests need that differs between versions
public class Platform {
    public static long emcValue(Item item) {
        return IEMCProxy.INSTANCE.getValue(item);
    }

    // ProjectE maps EMC when the server starts
    public static void prepareEmc(MinecraftServer server) {}

    public static boolean hasAdvancement(MinecraftServer server, ResourceLocation id) {
        return server.getAdvancements().getAdvancement(id) != null;
    }

    // the game test server gives ops a permission level of 0, so it's overridden instead
    public static ServerPlayer newPlayer(ServerLevel level, GameProfile profile, int permissionLevel) {
        return new ServerPlayer(level.getServer(), level, profile) {
            @Override
            protected int getPermissionLevel() {
                return permissionLevel;
            }
        };
    }
}
