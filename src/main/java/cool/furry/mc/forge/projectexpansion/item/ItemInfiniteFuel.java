package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ItemInfiniteFuel extends Item {
    public static final Supplier<BigInteger> COST = () -> BigInteger.valueOf(ProjectEAPI.getEMCProxy().getValue(Items.COAL));
    public static final int BURN_TIME = 1600;

    public ItemInfiniteFuel() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("item.projectexpansion.infinite_fuel.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("text.projectexpansion.cost", EMCFormat.getComponent(BURN_TIME).setStyle(ColorStyle.GRAY)).setStyle(ColorStyle.RED));
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID("Owner");
        if (owner == null)
            return 0;
        return ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner).getEmc().compareTo(COST.get()) < 0 ? 0 : BURN_TIME;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID("Owner");
        if (owner == null)
            return stack;
        ServerPlayer player = Util.getPlayer(owner);
        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(owner);
        provider.setEmc(provider.getEmc().subtract(COST.get()));
        if (player != null)
            provider.syncEmc(player);
        return stack;
    }
}
