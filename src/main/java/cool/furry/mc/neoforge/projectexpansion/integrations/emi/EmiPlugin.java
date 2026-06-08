package cool.furry.mc.neoforge.projectexpansion.integrations.emi;

import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.neoforge.projectexpansion.util.SearchSync;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        SearchSync.register(new SearchSync("emi", EmiApi::setSearchText));
        registry.addRecipeHandler(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), new ArcaneCraftingTransferHandler());
    }
}
