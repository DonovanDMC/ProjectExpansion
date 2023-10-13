package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.util.Objects;

public class FuelBlockItem extends BlockItem {
    private final Fuel level;
    public FuelBlockItem(Fuel level) {
        super(Objects.requireNonNull(Objects.requireNonNull(level).getBlock()), new Properties().tab(Main.tab).rarity(level.getRarity()));
        this.level = level;
    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        // 9 single items combined
        return level.getBurnTime(recipeType) * 9;
    }
}
