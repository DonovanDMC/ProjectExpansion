package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.forge.projectexpansion.util.IEmcStorageBigInteger;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public class Capabilities {
    @CapabilityInject(CapabilityAlchemicalBookLocations.Storage.class)
    public static Capability<CapabilityAlchemicalBookLocations.Storage> ALCHEMICAL_BOOK_LOCATIONS = null;
    @CapabilityInject(IEmcStorageBigInteger.class)
    public static Capability<IEmcStorageBigInteger> BIG_EMC_STORAGE_CAPABILITY = null;

    // capabilities need a storage and default instance before 1.17, ours handle saving themselves
    public static void register() {
        CapabilityManager.INSTANCE.register(CapabilityAlchemicalBookLocations.Storage.class, new NoStorage<>(), CapabilityAlchemicalBookLocations.Storage::new);
        CapabilityManager.INSTANCE.register(IEmcStorageBigInteger.class, new NoStorage<>(), () -> {
            throw new UnsupportedOperationException("IEmcStorageBigInteger has no default implementation");
        });
    }

    private static class NoStorage<T> implements Capability.IStorage<T> {
        @Nullable
        @Override
        public INBT writeNBT(Capability<T> capability, T instance, Direction side) {
            return null;
        }

        @Override
        public void readNBT(Capability<T> capability, T instance, Direction side, INBT nbt) {}
    }
}
