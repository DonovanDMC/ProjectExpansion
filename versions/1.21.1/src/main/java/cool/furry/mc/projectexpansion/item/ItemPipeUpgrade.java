package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.pipe.PipeSettings;
import cool.furry.mc.projectexpansion.util.pipe.PipeUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemPipeUpgrade extends Item {
    public final PipeUpgrade tier;

    public ItemPipeUpgrade(PipeUpgrade tier) {
        super(new Item.Properties());
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        long rate = tier.getRate();
        tooltip.add(Lang.Items.PIPE_UPGRADE_RATE.translateColored(ChatFormatting.GRAY, rate == Long.MAX_VALUE ? Component.literal("∞") : EMCFormat.getComponent(rate)));
        tooltip.add(Lang.Items.PIPE_UPGRADE_UNLOCKS.extendColored(tier.canChangeDistribution ? "distribution" : "redstone", ChatFormatting.GRAY));
        if (stack.has(DataComponentTypes.PIPE_SETTINGS.get()) && !PipeSettings.DEFAULT.equals(stack.get(DataComponentTypes.PIPE_SETTINGS.get()))) {
            tooltip.add(Lang.Items.PIPE_UPGRADE_CONFIGURED.translateColored(ChatFormatting.YELLOW));
        }
    }
}
