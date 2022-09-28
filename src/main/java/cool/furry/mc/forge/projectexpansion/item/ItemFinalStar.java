package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemFinalStar extends Item {
    public ItemFinalStar() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Component.translatable("item.projectexpansion.final_star.tooltip").setStyle(ColorStyle.GRAY));
        list.add(Component.translatable("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}

