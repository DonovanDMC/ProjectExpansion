package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// the emc display in the corner of the screen, the method returns the BigInteger emc it shows
// the player's given that much, then it's left until the display's per second gain settles back to nothing
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmcDisplay {}
