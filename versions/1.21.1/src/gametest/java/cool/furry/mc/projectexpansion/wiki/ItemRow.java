package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// items in a row, drawn bigger than in an inventory, the method returns a List<ItemStack>
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ItemRow {
    // 1 is the normal 16 pixels
    int scale() default 3;
}
