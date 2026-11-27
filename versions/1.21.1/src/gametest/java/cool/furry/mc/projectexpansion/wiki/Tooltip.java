package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// an item's tooltip, the method returns the ItemStack
// it can take the ServerPlayer too, to set up things the tooltip shows like what they've learned, it's drawn once that's reached their client
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tooltip {}
