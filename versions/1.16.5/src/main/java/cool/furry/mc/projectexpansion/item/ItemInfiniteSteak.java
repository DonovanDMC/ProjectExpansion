package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.ColorStyle;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

public class ItemInfiniteSteak extends Item {
    @SuppressWarnings("unused")
    public ItemInfiniteSteak() {
        super(new Properties()
                .food(Foods.COOKED_BEEF)
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.INFINITE_STEAK_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.COST.translateColored(TextFormatting.RED, EMCFormat.getComponent(Config.server.infiniteSteakCost.get()).setStyle(ColorStyle.GRAY)));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Items.COOKED_BEEF.getUseDuration(stack);
    }

    @Nullable
    @Override
    public Food getFoodProperties() {
        return Items.COOKED_BEEF.getFoodProperties();
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (!player.canEat(false) || Config.server.infiniteSteakCost.get() == 0 || provider == null || provider.getEmc().compareTo(BigInteger.valueOf(Config.server.infiniteSteakCost.get())) < 0) return ActionResult.fail(stack);
        player.startUsingItem(hand);
        return ActionResult.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity) || level.isClientSide) return stack;
        ServerPlayerEntity player = ((ServerPlayerEntity) entity);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, player.getDisplayName()), true);
            return stack;
        }
        BigInteger emc = provider.getEmc().subtract(BigInteger.valueOf(Config.server.infiniteSteakCost.get()));
        if (emc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(Lang.Items.INFINITE_STEAK_NOT_ENOUGH_EMC.translateColored(TextFormatting.RED, new StringTextComponent(Config.server.infiniteSteakCost.get().toString())), true);
            return stack;
        }
        provider.setEmc(emc);
        provider.syncEmc(player);
        player.eat(level, new ItemStack(Items.COOKED_BEEF));
        return stack;
    }
}
