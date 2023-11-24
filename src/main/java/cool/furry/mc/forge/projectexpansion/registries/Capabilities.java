package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final Capability<IAlchemicalBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS = CapabilityManager.get(new CapabilityToken<>(){});
}
