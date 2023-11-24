package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
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

public class ItemFinalStarShard extends Item {
    public ItemFinalStarShard() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.FINAL_STAR_SHARD_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }
}

