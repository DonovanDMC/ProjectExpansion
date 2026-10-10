package cool.furry.mc.projectexpansion.util;

import cool.furry.mc.projectexpansion.Main;
import moze_intel.projecte.api.event.EMCRemapEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// projecte only started tracking this in 1.18
@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class EMCLoadIndex {
    private static int loadIndex = -1;

    @SubscribeEvent
    public static void onRemap(EMCRemapEvent event) {
        loadIndex++;
    }

    public static int get() {
        return loadIndex;
    }
}
