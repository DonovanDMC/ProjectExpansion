package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.item.ItemStar;
import cool.furry.mc.neoforge.projectexpansion.registries.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

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

    public Star prev() {
        return VALUES[(ordinal() - 1  + VALUES.length) % VALUES.length];
    }

    public Star next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public final String name;
    @Nullable
    private DeferredHolder<Item, ItemStar> itemMagnum = null;
    @Nullable
    private DeferredHolder<Item, ItemStar> itemColossal = null;
    private DeferredHolder<Item, ItemStar> itemGargantuan = null;
    Star(String name) {
        this.name = name;
    }

    @Nullable
    public Star getPrev() {
        return this == EIN ? null : VALUES[ordinal() - 1];
    }


    public @Nullable ItemStar asMagnumItem() { return itemMagnum == null ? null : itemMagnum.get(); }
    public @Nullable ItemStar asColossalItem() { return itemColossal == null ? null : itemColossal.get(); }
    public @Nullable ItemStar asGargantuanItem() { return itemGargantuan == null ? null : itemGargantuan.get(); }

    private void register(StarType reg) {
        switch (reg) {
            case MAGNUM -> itemMagnum = Items.Registry.register(String.format("magnum_star_%s", name), () -> new ItemStar(reg, this));
            case COLOSSAL -> itemColossal = Items.Registry.register(String.format("colossal_star_%s", name), () -> new ItemStar(reg, this));
            case GARGANTUAN -> itemGargantuan = Items.Registry.register(String.format("gargantuan_star_%s", name), () -> new ItemStar(reg, this));
        }
    }

    public static void registerAll() {
        Arrays.stream(StarType.values()).forEach(type -> Arrays.stream(VALUES).forEach(val -> val.register(type)));
    }

    public static void setAllCreativeTab(CreativeModeTab.Output output) {
        Arrays.stream(StarType.values()).forEach(type -> {
            Arrays.stream(VALUES).forEach(val -> val.setCreativeTab(output, type));
        });
    }

    private void setCreativeTab(CreativeModeTab.Output output, StarType type) {
        if (type == StarType.MAGNUM && itemMagnum != null) output.accept(itemMagnum.get());
        if (type == StarType.COLOSSAL && itemColossal != null) output.accept(itemColossal.get());
        if (type == StarType.GARGANTUAN && itemGargantuan != null) output.accept(itemGargantuan.get());
    }

    public enum StarType {
        MAGNUM,
        COLOSSAL,
        GARGANTUAN;

        public int getOffset() {
            return ordinal() * 6;
        }
    }
}
