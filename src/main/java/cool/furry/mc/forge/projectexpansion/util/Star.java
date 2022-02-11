package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.item.ItemColossalStar;
import cool.furry.mc.forge.projectexpansion.item.ItemMagnumStar;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;

@SuppressWarnings("unused")
public enum Star {
    EIN("ein"),
    ZWEI("zwei"),
    DREI("drei"),
    VIER("vier"),
    SPHERE("sphere"),
    OMEGA("omega");

    public static final Star[] VALUES = values();

    public final String name;
    @Nullable
    private RegistryObject<ItemMagnumStar> itemMagnum = null;
    @Nullable
    private RegistryObject<ItemColossalStar> itemColossal = null;
    Star(String name) {
        this.name = name;
    }

    @Nullable
    public Star getPrev() {
        return this == EIN ? null : VALUES[ordinal() - 1];
    }


    public @Nullable ItemMagnumStar asMagnumItem() { return itemMagnum == null ? null : itemMagnum.get(); }
    public @Nullable ItemColossalStar asColossalItem() { return itemColossal == null ? null : itemColossal.get(); }

    private void register() {
        itemMagnum = Items.Registry.register(String.format("magnum_star_%s", name), () -> new ItemMagnumStar(this));
        itemColossal = Items.Registry.register(String.format("colossal_star_%s", name), () -> new ItemColossalStar(this));
    }

    public static void registerAll() {
        Arrays.stream(VALUES).forEach(Star::register);
    }
}
