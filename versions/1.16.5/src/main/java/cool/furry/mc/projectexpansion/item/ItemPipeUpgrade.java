package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemPipeUpgrade extends Item {
    public final PipeUpgrade tier;

    public ItemPipeUpgrade(PipeUpgrade tier) {
        super(new Item.Properties());
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        long rate = tier.getRate();
        tooltip.add(Lang.Items.PIPE_UPGRADE_RATE.translateColored(TextFormatting.GRAY, rate == Long.MAX_VALUE ? new StringTextComponent("∞") : EMCFormat.getComponent(rate)));
        tooltip.add(Lang.Items.PIPE_UPGRADE_UNLOCKS.extendColored(tier.canChangeDistribution ? "distribution" : "redstone", TextFormatting.GRAY));
        if (PipeSettings.has(stack) && !PipeSettings.DEFAULT.equals(PipeSettings.get(stack))) {
            tooltip.add(Lang.Items.PIPE_UPGRADE_CONFIGURED.translateColored(TextFormatting.YELLOW));
        }
    }
}
