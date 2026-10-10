package cool.furry.mc.projectexpansion.gametest;

import com.mojang.authlib.GameProfile;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.Item;

import java.lang.reflect.Field;

// what the shared GameTests need that differs between versions
public class Platform {
    public static long emcValue(Item item) {
        return ProjectEAPI.getEMCProxy().getValue(item);
    }

    // ProjectE maps EMC when the server starts
    public static void prepareEmc(MinecraftServer server) {}

    public static boolean hasAdvancement(MinecraftServer server, ResourceLocation id) {
        return server.getAdvancements().getAdvancement(id) != null;
    }

    // the game test server gives ops a permission level of 0, so it's overridden instead
    public static ServerPlayer newPlayer(ServerLevel level, GameProfile profile, int permissionLevel) {
        addProfileCache(level.getServer());
        return new ServerPlayer(level.getServer(), level, profile, null) {
            @Override
            protected int getPermissionLevel() {
                return permissionLevel;
            }
        };
    }

    // the game test server has no profile cache, which placeNewPlayer needs before 1.20
    private static void addProfileCache(MinecraftServer server) {
        for (Field field : MinecraftServer.class.getDeclaredFields()) {
            if (field.getType() != Services.class) continue;
            try {
                field.setAccessible(true);
                Services services = (Services) field.get(server);
                if (services.profileCache() != null) return;
                field.set(server, new Services(services.sessionService(), services.serviceSignatureValidator(), services.profileRepository(), new GameProfileCache(services.profileRepository(), server.getFile("usercache.json"))));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Couldn't add a profile cache", e);
            }
            return;
        }
        throw new IllegalStateException("MinecraftServer has no services field");
    }

    public static boolean hasPlacedFeature(ServerLevel level, ResourceLocation id) {
        return level.registryAccess().registryOrThrow(net.minecraft.core.Registry.PLACED_FEATURE_REGISTRY).containsKey(id);
    }

    public static boolean hasOreModifier(ServerLevel level, ResourceLocation id) {
        return level.registryAccess().registryOrThrow(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(id);
    }
}
