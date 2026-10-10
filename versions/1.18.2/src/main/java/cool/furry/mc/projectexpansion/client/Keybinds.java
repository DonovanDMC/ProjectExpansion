package cool.furry.mc.projectexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static boolean REGISTERED = false;
    public static KeyMapping OPEN_TRANSMUTATION_TABLET;
    // held while clicking an item in a gui, not pressed on its own
    public static KeyMapping TOGGLE_EMC_LOCK;

    public static void register() {
        OPEN_TRANSMUTATION_TABLET = new KeyMapping(
                "key.projecte.curios.open_transmutation_tablet",
                GLFW.GLFW_KEY_K,
                PELang.PROJECTE.getTranslationKey()
        );
        TOGGLE_EMC_LOCK = new KeyMapping(
                "key.projectexpansion.toggle_emc_lock",
                KeyConflictContext.GUI,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                PELang.PROJECTE.getTranslationKey()
        );
        ClientRegistry.registerKeyBinding(OPEN_TRANSMUTATION_TABLET);
        ClientRegistry.registerKeyBinding(TOGGLE_EMC_LOCK);
        REGISTERED = true;
    }
}
