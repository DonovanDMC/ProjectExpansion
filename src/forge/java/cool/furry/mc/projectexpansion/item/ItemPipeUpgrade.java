package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemPipeUpgrade extends Item {
    public final PipeUpgrade tier;

    public ItemPipeUpgrade(PipeUpgrade tier) {
        super(new Item.Properties());
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        long rate = tier.getRate();
        tooltip.add(Lang.Items.PIPE_UPGRADE_RATE.translateColored(ChatFormatting.GRAY, rate == Long.MAX_VALUE ? Platform.literal("∞") : EMCFormat.getComponent(rate)));
        tooltip.add(Lang.Items.PIPE_UPGRADE_UNLOCKS.extendColored(tier.canChangeDistribution ? "distribution" : "redstone", ChatFormatting.GRAY));
        if (PipeSettings.has(stack) && !PipeSettings.DEFAULT.equals(PipeSettings.get(stack))) {
            tooltip.add(Lang.Items.PIPE_UPGRADE_CONFIGURED.translateColored(ChatFormatting.YELLOW));
        }
    }
}
