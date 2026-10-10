package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// what a wrench does is up to the block, see BlockEmcPipe.useWrench
public class ItemWrench extends Item {
    public ItemWrench() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Lang.Items.WRENCH_TOOLTIP.translateColored(ChatFormatting.GRAY));
    }
}
