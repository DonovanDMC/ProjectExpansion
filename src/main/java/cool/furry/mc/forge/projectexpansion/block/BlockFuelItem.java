package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.util.Objects;

public class BlockFuelItem extends BlockItem {
    private final Fuel level;
    public BlockFuelItem(Fuel level) {
        super(Objects.requireNonNull(Objects.requireNonNull(level).getBlock()), new Item.Properties().tab(Main.tab).rarity(level.rarity));
        this.level = level;
    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        // 9 single items combined
        return level.getBurnTime(recipeType) * 9;
    }
}
