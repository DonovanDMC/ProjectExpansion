package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// items in slots on a panel like an inventory, the method returns a List<List<ItemStack>> of rows
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ItemGrid {}
