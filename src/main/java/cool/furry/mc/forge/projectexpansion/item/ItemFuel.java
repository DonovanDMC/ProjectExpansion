package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public class ItemFuel extends Item  {
    private final Fuel level;
    public ItemFuel(Fuel level) {
        super(new Item.Properties().tab(Main.tab).rarity(level.rarity));
        this.level = level;

    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return level.getBurnTime();
    }
}
