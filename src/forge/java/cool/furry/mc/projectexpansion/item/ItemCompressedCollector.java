package cool.furry.mc.projectexpansion.item;

import cool.furry.mc.projectexpansion.util.IHasMatter;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Matter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.COMRESSED_COLLECTOR_TOOLTIP.translateColored(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
