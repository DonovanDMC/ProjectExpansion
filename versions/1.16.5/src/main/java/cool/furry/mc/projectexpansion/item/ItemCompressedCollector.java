package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.IHasMatter;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCompressedCollector extends Item implements IHasMatter {
    public final Matter matter;
    public ItemCompressedCollector(Matter matter) {
        super(new Properties().rarity(matter.getRarity()));
        this.matter = matter;
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.COMRESSED_COLLECTOR_TOOLTIP.translateColored(TextFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
