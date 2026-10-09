package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;

import javax.annotation.Nullable;
import java.util.Objects;

public class FuelBlockItem extends BlockItem {
    private final Fuel level;
    public FuelBlockItem(Fuel level) {
        super(Objects.requireNonNull(Objects.requireNonNull(level).getBlock()), new Properties().rarity(level.getRarity()));
        this.level = level;
    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable IRecipeType<?> recipeType) {
        // 9 single items combined
        return level.getBurnTime(recipeType) * 9;
    }
}
