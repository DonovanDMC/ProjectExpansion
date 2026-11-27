package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// the toast that shows in the top right when the advancement is made
// the method takes nothing and does nothing, everything's here
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Advancement {
    // an advancement id, in this mod's namespace unless it has one
    String[] value();

    // how many toasts side by side, filling each row before the next
    int columns() default 1;
}
