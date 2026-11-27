package cool.furry.mc.projectexpansion.wiki;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.SharedLibrary;

// holding keys down the way a real keyboard would, for menus that look different with shift held
// the game reads keys straight from the window, so they're pressed on the (virtual) display through xtest
final class WikiKeys {
    // the x keysym for the left shift key
    private static final long SHIFT = 0xFFE1;
    private static SharedLibrary xtst, x11;
    private static boolean held;

    private WikiKeys() {}

    static void shift(boolean down) {
        if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_X11) throw new IllegalStateException("Holding keys only works on x11, like the virtual display");
        if (xtst == null) {
            xtst = Library.loadNative(WikiKeys.class, "org.lwjgl", "libXtst.so.6");
            x11 = Library.loadNative(WikiKeys.class, "org.lwjgl", "libX11.so.6");
        }
        long display = GLFWNativeX11.glfwGetX11Display();
        int keycode = JNI.invokePPI(display, SHIFT, x11.getFunctionAddress("XKeysymToKeycode")) & 0xFF;
        JNI.invokePPI(display, keycode, down ? 1 : 0, 0L, xtst.getFunctionAddress("XTestFakeKeyEvent"));
        JNI.invokePI(display, x11.getFunctionAddress("XFlush"));
        held = down;
    }

    // so it isn't left held when an image fails partway
    static void release() {
        if (held) shift(false);
    }

    // whether the game sees it, once the window's had the event
    static boolean shiftDown() {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
