package cool.furry.mc.projectexpansion.integrations.emi;

import cool.furry.mc.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.projectexpansion.registries.MenuTypes;
import cool.furry.mc.projectexpansion.util.SearchSync;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;

@EmiEntrypoint
public class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        SearchSync.register(new SearchSync("emi", EmiApi::setSearchText));
        registry.addRecipeHandler(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), new ArcaneCraftingTransferHandler());
        registry.addExclusionArea(GUIArcaneTransmutationTablet.class, (screen, consumer) -> {
            for (Rect2i area : screen.getExtraAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
    }
}
