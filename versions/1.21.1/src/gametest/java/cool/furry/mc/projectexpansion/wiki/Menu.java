package cool.furry.mc.projectexpansion.wiki;

import net.minecraft.client.gui.screens.Screen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// a menu, the whole window with it open, or just the menu with WikiImage's cutout
// the method takes (ServerPlayer, BlockPos), sets up what the menu needs around that spot on the server,
// and returns a Runnable that opens it, which is run once the client has the blocks
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Menu {
    // the screen that opens
    Class<? extends Screen> screen();

    // how long to wait once it's open, for anything that fills in or changes over time
    int settleTicks() default 5;

    // where the mouse is, {x, y} in gui pixels from the menu's top left (the window's for a screen that isn't a menu), like to show a slot's tooltip
    int[] mouse() default {};

    // just part of the window, {x, y, width, height} in gui pixels measured like mouse, which can reach past the menu for a tooltip
    int[] crop() default {};

    // shift held down, for buttons that change while it is
    boolean shift() default false;
}
