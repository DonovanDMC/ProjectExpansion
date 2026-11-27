package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marks a method as making a wiki image, along with one of Recipe, Tooltip, Advancement, ItemRow, ItemGrid, EmcDisplay, Command, Menu or Scene for how it's made
// the method has to be static, in one of WikiImages.DEFINITIONS' classes under wiki.images
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WikiImage {
    // where it's saved, relative to the wiki's images folder, ex: "EMC-Link/all-tiers.png"
    String value();

    // a transparent background, cropped to what's left, rather than whatever was behind it
    // for scenes that's the sky, and for menus it's the game behind them, recipes, tooltips, advancements & items never have anything behind them
    boolean cutout() default false;

    // emc numbers shown in full, like when shift's held, rather than shortened like 1.2 Million, by turning off the formatEMC config while it's made
    boolean fullEmc() default false;
}
