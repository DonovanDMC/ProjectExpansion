package cool.furry.mc.neoforge.projectexpansion.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class DamageSources {
    private final Registry<DamageType> damageTypes;
    private final DamageSource walkOnSun;
    private final DamageSource stareAtSun;
    public DamageSources(RegistryAccess registry) {
        this.damageTypes = registry.registryOrThrow(Registries.DAMAGE_TYPE);
        this.walkOnSun = this.source(DamageTypes.WALK_ON_SUN);
        this.stareAtSun = this.source(DamageTypes.STARE_AT_SUN);
    }

    private DamageSource source(ResourceKey<DamageType> damageType) {
        return new DamageSource(this.damageTypes.getHolderOrThrow(damageType));
    }

    private DamageSource source(ResourceKey<DamageType> damageType, @Nullable Entity causingEntity) {
        return new DamageSource(this.damageTypes.getHolderOrThrow(damageType), causingEntity);
    }

    private DamageSource source(ResourceKey<DamageType> damageType, @Nullable Entity causingEntity, @Nullable Entity directEntity) {
        return new DamageSource(this.damageTypes.getHolderOrThrow(damageType), causingEntity, directEntity);
    }

    public DamageSource walkOnSun() {
        return this.walkOnSun;
    }

    public DamageSource stareAtSun() {
        return this.stareAtSun;
    }

    public static DamageSources fromServer(MinecraftServer server) {
        return new DamageSources(server.registryAccess());
    }

    public static DamageSources fromLevel(Level level) {
        return new DamageSources(level.registryAccess());
    }
}
