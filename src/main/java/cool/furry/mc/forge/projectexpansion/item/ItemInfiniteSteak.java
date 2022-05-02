package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
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
import java.util.function.Supplier;

public class ItemInfiniteSteak extends Item {
    public static final Supplier<BigInteger> COST = () -> BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.COOKED_BEEF));

    public ItemInfiniteSteak() {
        super(new Item.Properties().maxStackSize(1).rarity(Rarity.RARE).group(Main.group));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, world, list, flag);
        list.add(new TranslationTextComponent("item.projectexpansion.infinite_steak.tooltip").setStyle(ColorStyle.GRAY));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Items.COOKED_BEEF.getUseDuration(stack);
    }

    @Override
    public boolean isFood() {
        return true;
    }

    @Nullable
    @Override
    public Food getFood() {
        return Items.COOKED_BEEF.getFood();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        // TODO: wolves???
        if (!player.canEat(false) || ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID()).getEmc().compareTo(COST.get()) < 0)
            return ActionResult.func_226251_d_(stack);
        player.setActiveHand(hand);
        return world.isRemote ? ActionResult.func_226248_a_(stack) : ActionResult.func_226249_b_(stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity) || world.isRemote)
            return stack;
        ServerPlayerEntity player = (ServerPlayerEntity) entity;
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(entity.getUniqueID());
        BigInteger emc = provider.getEmc().subtract(COST.get());
        if (emc.compareTo(BigInteger.ZERO) < 0) {
            player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.infinite_steak.not_enough_emc", new StringTextComponent(COST.get().toString())).setStyle(ColorStyle.RED), true);
            return stack;
        }
        provider.setEmc(emc);
        provider.sync(player);
        player.onFoodEaten(world, new ItemStack(Items.COOKED_BEEF));
        return stack;
    }
}
