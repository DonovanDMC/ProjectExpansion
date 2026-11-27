package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// crafting or refining recipes, stacked when there's more than one
// the method takes nothing and does nothing, everything's here
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Recipe {
    // recipe ids, in this mod's namespace unless they have one
    String[] value();
}
