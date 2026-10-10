package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.Fuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;

import javax.annotation.Nullable;

public class ItemFuel extends Item  {
    private final Fuel level;
    public ItemFuel(Fuel level) {
        super(new Properties().rarity(level.getRarity()));
        this.level = level;
    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable IRecipeType<?> recipeType) {
        return level.getBurnTime(recipeType);
    }
}
