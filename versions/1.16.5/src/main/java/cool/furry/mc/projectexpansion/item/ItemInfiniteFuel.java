package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.ColorStyle;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.OwnerData;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, list, tooltipFlag);
        list.add(Lang.Items.INFINITE_FUEL_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.COST.translateColored(TextFormatting.RED, EMCFormat.getComponent(Config.server.infiniteFuelCost.get()).setStyle(ColorStyle.GRAY)));
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable IRecipeType<?> recipeType) {
        @Nullable OwnerData owner = OwnerData.get(stack);
        @Nullable IKnowledgeProvider provider = owner == null ? null : Util.getKnowledgeProvider(owner.uuid());
        if (owner == null || provider == null) return 0;
        return (Config.server.infiniteFuelCost.get() == 0 || Config.server.infiniteFuelBurnTime.get() == 0) ? 0 : provider.getEmc().compareTo(BigInteger.valueOf(Config.server.infiniteFuelCost.get())) < 0 ? 0 : Config.server.infiniteFuelBurnTime.get();
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        OwnerData ownerData = OwnerData.get(stack);
        @Nullable UUID owner = ownerData == null ? null : ownerData.uuid();
        if (owner == null)
            return stack;
        ServerPlayerEntity player = Util.getPlayer(owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return stack;
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(Config.server.infiniteFuelCost.get())));
        if (player != null) provider.syncEmc(player);
        return stack;
    }
}
