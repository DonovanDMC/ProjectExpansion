package cool.furry.mc.projectexpansion.registries;

import cool.furry.mc.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.projectexpansion.util.IEmcStorageBigInteger;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final Capability<CapabilityAlchemicalBookLocations.Storage> ALCHEMICAL_BOOK_LOCATIONS = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<IEmcStorageBigInteger> BIG_EMC_STORAGE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
}
