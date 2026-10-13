package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// what a wrench does is up to the block, see BlockEmcPipe.useWrench
public class ItemWrench extends Item {
    public ItemWrench() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Lang.Items.WRENCH_TOOLTIP.translateColored(TextFormatting.GRAY));
    }
}
