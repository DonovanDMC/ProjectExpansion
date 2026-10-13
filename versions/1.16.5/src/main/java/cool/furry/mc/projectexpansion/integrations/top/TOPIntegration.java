package cool.furry.mc.projectexpansion.integrations.top;

import net.minecraftforge.fml.InterModComms;

public class TOPIntegration {
    public static final String MOD_ID = "theoneprobe";

    public static void sendIMC() {
        InterModComms.sendTo(MOD_ID, "getTheOneProbe", ProbeInfoProvider::new);
    }
}
