package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

public class ItemInfiniteSteak extends Item {

    public ItemInfiniteSteak() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);
        list.add(new TranslationTextComponent("item.projectexpansion.infinite_steak.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.cost", EMCFormat.getComponent(Config.infiniteSteakCost.get()).setStyle(ColorStyle.GRAY)).setStyle(ColorStyle.RED));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Items.COOKED_BEEF.getUseDuration(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEdible() {
        return true;
    }

    @Nullable
    @Override
    public Food getFoodProperties() {
        return Items.COOKED_BEEF.getFoodProperties();
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (!player.canEat(false) || Config.infiniteSteakCost.get() == 0 || provider == null || provider.getEmc().compareTo(BigInteger.valueOf(Config.infiniteSteakCost.get())) < 0) return ActionResult.fail(stack);
        player.startUsingItem(hand);
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity) || level.isClientSide) return stack;
        ServerPlayerEntity player = (ServerPlayerEntity) entity;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            player.displayClientMessage(new TranslationTextComponent("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED), true);
            return stack;
        }
        BigInteger emc = provider.getEmc().subtract(BigInteger.valueOf(Config.infiniteSteakCost.get()));
        if (emc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.infinite_steak.not_enough_emc", new StringTextComponent(Config.infiniteSteakCost.get().toString())).setStyle(ColorStyle.RED), true);
            return stack;
        }
        provider.setEmc(emc);
        provider.syncEmc(player);
        player.eat(level, new ItemStack(Items.COOKED_BEEF));
        return stack;
    }
}
