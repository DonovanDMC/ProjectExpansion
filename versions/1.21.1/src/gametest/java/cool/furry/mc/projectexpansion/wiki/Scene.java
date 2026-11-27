package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// blocks placed in an empty sky, with WikiImage's cutout for a transparent background cropped to them
// the method takes (ServerLevel, BlockPos) and builds the scene from that spot, the camera frames whatever it built
// or it takes (ServerPlayer, BlockPos), can set up the player's hotbar too, and returns a Runnable that's run on the server once the camera's
// in place and everything's drawn, like using an item on a block, which is saved a moment later so its message shows
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scene {
    // how much of the view's height the scene takes up
    double fill() default 0.5;

    View view() default View.CORNER;

    // override the view's angle, in degrees
    float yaw() default Float.NaN;

    float pitch() default Float.NaN;

    // in ticks, 6000 is noon & 18000 is midnight
    long time() default 6000;

    // with cutout, this only changes the light, rain itself isn't drawn since it'd cover the transparent background
    Weather weather() default Weather.CLEAR;

    // a box to frame instead of what was built, {x1, y1, z1, x2, y2, z2} in blocks from the scene's spot, and with cutout it's cropped to the box
    // rather than to what's there, so scenes with the same box line up exactly, like a block open & closed
    double[] frame() default {};

    // a hotbar slot (0 to 8) to catch partway through the squish an item does when it lands in the inventory, once the action's done
    int pop() default -1;

    // how many ticks after the action it's saved, enough for things like a chest lid to open, but a message above the hotbar starts fading at 40
    int actionTicks() default 15;

    // the hotbar, crosshair, held item & messages, like playing, which can't be cut out so only works without cutout
    boolean hud() default false;

    enum View {
        // looking down from the corner towards negative x & z, so north & west faces show
        CORNER(-45, 45),
        // straight at the north faces, which is where blocks face by default
        FRONT(0, 0),
        // straight down
        TOP(0, 90);

        public final float yaw, pitch;

        View(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    enum Weather {
        CLEAR(0, 0),
        RAIN(1, 0),
        THUNDER(1, 1);

        public final float rain, thunder;

        Weather(float rain, float thunder) {
            this.rain = rain;
            this.thunder = thunder;
        }
    }
}
