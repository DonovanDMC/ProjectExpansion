package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class Capabilities {
    @CapabilityInject(IAlchemicalBookLocationsProvider.class)
    public static final Capability<IAlchemicalBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS = null;
}
