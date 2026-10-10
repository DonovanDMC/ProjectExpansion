package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.ArcaneTransmutationContainerProvider;
import cool.furry.mc.projectexpansion.util.ContainerData;
import cool.furry.mc.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class ItemArcaneTransmutationTablet extends Item implements ITransmutationTablet {
    public ItemArcaneTransmutationTablet() {
        super(new Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Items.ARCANE_TRANSMUTATION_TABLET_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            openContainer(player, hand, player.getInventory().selected);
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void openContainer(Player player, InteractionHand hand, int selected) {
        player.openMenu(new ArcaneTransmutationContainerProvider(hand, Lang.GUI.ARCANE_TRANSMUTATION_TABLET.translate()), (buf) -> ContainerData.inHand(buf, hand, selected));
    }

    @Override
    public void openContainer(Player player) {
        player.openMenu(new ArcaneTransmutationContainerProvider(null, Lang.GUI.ARCANE_TRANSMUTATION_TABLET.translate()), ContainerData::noHand);
    }
}
