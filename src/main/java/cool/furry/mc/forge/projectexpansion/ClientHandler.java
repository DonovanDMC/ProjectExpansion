package cool.furry.mc.forge.projectexpansion;

import cool.furry.mc.forge.projectexpansion.config.ConfigMenu;
import cool.furry.mc.forge.projectexpansion.gui.ScreenMatterReplicator;
import cool.furry.mc.forge.projectexpansion.init.ContainerTypes;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigMenu(screen));
        ScreenManager.registerFactory(ContainerTypes.MATTER_REPLICATOR.get(), ScreenMatterReplicator::new);
    }
}
