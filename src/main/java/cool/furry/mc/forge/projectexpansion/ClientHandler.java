package cool.furry.mc.forge.projectexpansion;

import cool.furry.mc.forge.projectexpansion.config.ConfigMenu;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.utils.Constants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigMenu(screen));
        // ClientRegistry.bindTileEntityRenderer(TileEntityTypes.EMC_LINK.get(), RenderTileEMCLink::new);
        replaceEMCFormatter();
    }

    static void replaceEMCFormatter() {
        try {
            Field field = Constants.class.getDeclaredField("EMC_FORMATTER");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, EMCFormat.INSTANCE);
        } catch (NoSuchFieldException | IllegalAccessException ignore) {}
    }
}