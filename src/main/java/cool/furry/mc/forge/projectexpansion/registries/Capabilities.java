package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.capability.IAlchemialBookLocationsProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final Capability<IAlchemialBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS = CapabilityManager.get(new CapabilityToken<>(){});
}
