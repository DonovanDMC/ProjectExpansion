package cool.furry.mc.projectexpansion.client;

import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static boolean REGISTERED = false;
    public static KeyBinding OPEN_TRANSMUTATION_TABLET;
    // held while clicking an item in a gui, not pressed on its own
    public static KeyBinding TOGGLE_EMC_LOCK;

    public static void register() {
        OPEN_TRANSMUTATION_TABLET = new KeyBinding(
                "key.projecte.curios.open_transmutation_tablet",
                GLFW.GLFW_KEY_K,
                PELang.PROJECTE.getTranslationKey()
        );
        TOGGLE_EMC_LOCK = new KeyBinding(
                "key.projectexpansion.toggle_emc_lock",
                KeyConflictContext.GUI,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                PELang.PROJECTE.getTranslationKey()
        );
        ClientRegistry.registerKeyBinding(OPEN_TRANSMUTATION_TABLET);
        ClientRegistry.registerKeyBinding(TOGGLE_EMC_LOCK);
        REGISTERED = true;
    }
}
