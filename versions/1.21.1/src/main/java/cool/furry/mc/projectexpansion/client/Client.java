package cool.furry.mc.projectexpansion.client;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.GUIEmcDisplayEditor;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Context;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Element;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import javax.annotation.Nullable;

@Mod(value = Main.MOD_ID, dist = Dist.CLIENT)
public class Client {
    public Client(ModContainer modContainer, IEventBus eventBus) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new ConfigurationScreen(container, parent, Client::filterConfig));
    }

    // the emc display is positioned by dragging it around, so its values are swapped for a button that opens the editor
    private static @Nullable Element filterConfig(Context context, String key, Element original) {
        if (context.modSpec() != Config.client.Spec) return original;
        return switch (key) {
            case "emcDisplayPosition" -> new Element(original.name(), original.tooltip(), Button.builder(Lang.GUI.EMC_DISPLAY_EDITOR_EDIT.translate(), (button) -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new GUIEmcDisplayEditor(mc.screen));
            }).width(Button.DEFAULT_WIDTH).build(), false);
            case "emcDisplayOffsetX", "emcDisplayOffsetY" -> null;
            default -> original;
        };
    }
}
