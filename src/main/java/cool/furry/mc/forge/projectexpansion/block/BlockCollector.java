package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCollector extends Block implements IHasMatter, EntityBlock {
    private final Matter matter;

    public BlockCollector(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(3.5F));
        this.matter = matter;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCollector(pos, state);
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("block.projectexpansion.collector.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("block.projectexpansion.collector.emc", TransmutationEMCFormatter.formatEMC(matter.getCollectorOutputForTicks(Config.tickDelay.get())).copy().setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.GRAY));
    }
}
