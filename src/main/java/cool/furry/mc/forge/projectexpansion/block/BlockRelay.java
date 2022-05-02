package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockRelay extends Block implements IHasMatter {
    private final Matter matter;

    public BlockRelay(Matter matter) {
        super(Block.Properties.create(Material.ROCK).hardnessAndResistance(5F));
        this.matter = matter;
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileRelay();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add(new TranslationTextComponent("block.projectexpansion.relay.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
        list.add(new TranslationTextComponent("block.projectexpansion.relay.bonus", new StringTextComponent(TransmutationEMCFormatter.formatEMC(matter.getRelayBonusForTicks(Config.tickDelay.get()))).setStyle(ColorStyle.GREEN).setStyle(ColorStyle.GRAY)));
        list.add(new TranslationTextComponent("block.projectexpansion.relay.transfer", new StringTextComponent(TransmutationEMCFormatter.formatEMC(matter.getRelayTransferForTicks(Config.tickDelay.get()))).setStyle(ColorStyle.GREEN).setStyle(ColorStyle.GRAY)));
    }

    @Override
    public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type) {
        return false;
    }
}
