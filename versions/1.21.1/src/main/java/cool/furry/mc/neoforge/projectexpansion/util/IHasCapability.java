package cool.furry.mc.neoforge.projectexpansion.util;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// ProjectE has similar functionality, but I feel like it's too internal to rely on
@FunctionalInterface
public interface IHasCapability {
    void registerCapabilities(RegisterCapabilitiesEvent event);
}
