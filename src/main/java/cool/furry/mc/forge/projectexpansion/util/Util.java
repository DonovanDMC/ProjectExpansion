package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class Util {
    // for some reason this only exists on net.minecraft.util.Util in 1.16+
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayerEntity getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
    }
}