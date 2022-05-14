package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Supplier;

public class ItemInfiniteSteak extends Item {
    public static final Supplier<BigInteger> COST = () -> BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.COOKED_BEEF));

    public ItemInfiniteSteak() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("item.projectexpansion.infinite_steak.tooltip").setStyle(ColorStyle.GRAY));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Items.COOKED_BEEF.getUseDuration(stack);
    }

    @Override
    public boolean isEdible() {
        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return Items.COOKED_BEEF.getFoodProperties(stack, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // TODO: wolves???
        if (!player.canEat(false) || ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID()).getEmc().compareTo(COST.get()) < 0)
            return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || level.isClientSide) return stack;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        BigInteger emc = provider.getEmc().subtract(COST.get());
        if (emc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(new TranslatableComponent("item.projectexpansion.infinite_steak.not_enough_emc", new TextComponent(COST.get().toString())).setStyle(ColorStyle.RED), true);
            return stack;
        }
        provider.setEmc(emc);
        provider.syncEmc(player);
        player.eat(level, new ItemStack(Items.COOKED_BEEF));
        return stack;
    }
}
