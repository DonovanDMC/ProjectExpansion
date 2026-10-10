package cool.furry.mc.projectexpansion.registries;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

// damage types aren't data driven before 1.19.4, so these are plain sources with the same message ids
public class DamageSources {
    private static final DamageSources INSTANCE = new DamageSources();
    private final DamageSource walkOnSun = new DamageSource("walk_on_sun");
    private final DamageSource stareAtSun = new DamageSource("stare_at_sun");

    public DamageSource walkOnSun() {
        return this.walkOnSun;
    }

    public DamageSource stareAtSun() {
        return this.stareAtSun;
    }

    public static DamageSources fromServer(MinecraftServer server) {
        return INSTANCE;
    }

    public static DamageSources fromLevel(Level level) {
        return INSTANCE;
    }
}
