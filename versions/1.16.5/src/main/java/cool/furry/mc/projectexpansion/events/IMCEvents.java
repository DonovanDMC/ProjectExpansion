package cool.furry.mc.projectexpansion.events;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.integrations.curios.CuriosIntegration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IMCEvents {
    @SubscribeEvent
    public static void interModEnqueueEvent(InterModEnqueueEvent event) {
        ModList modList = ModList.get();
        CuriosIntegration.sendIMC();
    }
}
