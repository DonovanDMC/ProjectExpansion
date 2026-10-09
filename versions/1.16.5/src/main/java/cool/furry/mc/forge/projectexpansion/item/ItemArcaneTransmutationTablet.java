package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.util.ArcaneTransmutationContainerProvider;
import cool.furry.mc.forge.projectexpansion.util.ContainerData;
import cool.furry.mc.forge.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemArcaneTransmutationTablet extends Item implements ITransmutationTablet {
    public ItemArcaneTransmutationTablet() {
        super(new Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, list, tooltipFlag);
        list.add(Lang.Items.ARCANE_TRANSMUTATION_TABLET_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        if (!level.isClientSide) {
            openContainer(player, hand, player.inventory.selected);
        }

        return ActionResult.success(player.getItemInHand(hand));
    }

    @Override
    public void openContainer(PlayerEntity player, Hand hand, int selected) {
        NetworkHooks.openGui((ServerPlayerEntity) player, new ArcaneTransmutationContainerProvider(hand, Lang.GUI.ARCANE_TRANSMUTATION_TABLET.translate()), (buf) -> ContainerData.inHand(buf, hand, selected));
    }

    @Override
    public void openContainer(PlayerEntity player) {
        NetworkHooks.openGui((ServerPlayerEntity) player, new ArcaneTransmutationContainerProvider(null, Lang.GUI.ARCANE_TRANSMUTATION_TABLET.translate()), ContainerData::noHand);
    }
}
