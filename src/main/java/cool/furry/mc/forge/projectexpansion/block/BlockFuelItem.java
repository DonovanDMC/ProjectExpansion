package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.Fuel;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public class BlockFuelItem extends BlockItem {
    private final Fuel level;
    public BlockFuelItem(Fuel level) {
        super(Objects.requireNonNull(Objects.requireNonNull(level).getBlock()), new Item.Properties().tab(Main.tab).rarity(level.rarity));
        this.level = level;

    }


    @Override
    @SuppressWarnings("deprecation")
    public int getBurnTime(ItemStack stack) {
        // 9 single items combined
        return level.getBurnTime() * 9;
    }
}
