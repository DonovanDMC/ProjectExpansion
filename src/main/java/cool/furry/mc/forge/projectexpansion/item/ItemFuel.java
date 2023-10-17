package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemFuel extends Item {
    private final Fuel level;
    public ItemFuel(Fuel level) {
        super(new Item.Properties().tab(Main.tab).rarity(level.rarity));
        this.level = level;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getBurnTime(ItemStack stack) {
        return level.getBurnTime();
    }
}
