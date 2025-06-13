package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICollector;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
        event.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
        event.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
        event.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
        event.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
    }
}
