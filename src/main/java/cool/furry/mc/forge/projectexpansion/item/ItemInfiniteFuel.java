package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);
        list.add(Lang.Items.INFINITE_FUEL_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.COST.translateColored(TextFormatting.RED, EMCFormat.getComponent(Config.infiniteFuelCost.get()).setStyle(ColorStyle.GRAY)));
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable IRecipeType<?> recipeType) {
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID(NBTNames.OWNER);
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
        @Nullable UUID owner = stack.getTag() == null ? null : stack.getTag().getUUID(NBTNames.OWNER);
        if (owner == null) return stack;
        ServerPlayerEntity player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return stack;
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(Config.infiniteFuelCost.get())));
        if (player != null) provider.syncEmc(player);
        return stack;
    }
}
