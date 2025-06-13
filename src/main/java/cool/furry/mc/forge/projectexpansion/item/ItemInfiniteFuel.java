package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class ItemInfiniteFuel extends Item {

    public ItemInfiniteFuel() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Items.INFINITE_FUEL_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.COST.translateColored(ChatFormatting.RED, EMCFormat.getComponent(Config.server.infiniteFuelCost.get()).setStyle(ColorStyle.GRAY)));
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        @Nullable DataComponentTypes.OwnerData owner = stack.get(DataComponentTypes.OWNER);
        @Nullable IKnowledgeProvider provider = owner == null ? null : Util.getKnowledgeProvider(owner.uuid());
        if (owner == null || provider == null) return 0;
        return (Config.server.infiniteFuelCost.get() == 0 || Config.server.infiniteFuelBurnTime.get() == 0) ? 0 : provider.getEmc().compareTo(BigInteger.valueOf(Config.server.infiniteFuelCost.get())) < 0 ? 0 : Config.server.infiniteFuelBurnTime.get();
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        DataComponentTypes.OwnerData ownerData = stack.get(DataComponentTypes.OWNER);
        @Nullable UUID owner = ownerData == null ? null : ownerData.uuid();
        if (owner == null)
            return stack;
        ServerPlayer player = Util.getServerPlayer(owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return stack;
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(Config.server.infiniteFuelCost.get())));
        if (player != null) provider.syncEmc(player);
        return stack;
    }
}
