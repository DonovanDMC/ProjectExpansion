package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class ItemInfiniteFuel extends Item {

    public ItemInfiniteFuel() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("item.projectexpansion.infinite_fuel.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("text.projectexpansion.cost", EMCFormat.getComponent(Config.infiniteFuelCost.get()).setStyle(ColorStyle.GRAY)).setStyle(ColorStyle.RED));
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID(TagNames.OWNER);
        @Nullable IKnowledgeProvider provider = owner == null ? null : Util.getKnowledgeProvider(owner);
        if (owner == null || provider == null) return 0;
        return (Config.infiniteFuelCost.get() == 0 || Config.infiniteFuelBurnTime.get() == 0) ? 0 : provider.getEmc().compareTo(BigInteger.valueOf(Config.infiniteFuelCost.get())) < 0 ? 0 : Config.infiniteFuelBurnTime.get();
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID(TagNames.OWNER);
        if (owner == null)
            return stack;
        ServerPlayer player = Util.getPlayer(owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return stack;
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(Config.infiniteFuelCost.get())));
        if (player != null) provider.syncEmc(player);
        return stack;
    }
}
