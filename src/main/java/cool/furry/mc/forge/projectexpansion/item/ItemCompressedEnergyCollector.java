package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCompressedEnergyCollector extends Item {
    public final Matter matter;

    public ItemCompressedEnergyCollector(Matter matter) {
        super(new Item.Properties().group(Main.group).rarity(matter.rarity));
        this.matter = matter;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, world, list, flag);
        list.add(new TranslationTextComponent("item.projectexpansion.compressed_collector.tooltip").setStyle(ColorStyle.GRAY));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
