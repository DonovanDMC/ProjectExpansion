package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayerEntity getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static @Nullable
    ServerPlayerEntity getPlayer(@Nullable World world, UUID uuid) {
        return world == null || world.getServer() == null ? null : world.getServer().getPlayerList().getPlayerByUUID(uuid);
    }

    public static boolean isWorldRemoteOrNull(@Nullable World world) {
        return world == null || world.isRemote;
    }
}
