package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TileEMCLink;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockEMCLink extends HorizontalBlock implements IHasMatter {
    private final Matter matter;

    public BlockEMCLink(Matter matter) {
        super(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(3.5F));
        this.matter = matter;
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileEMCLink();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add((new TranslationTextComponent("block.projectexpansion.emc_link.tooltip")).mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").mergeStyle(TextFormatting.AQUA));
        list.add((new TranslationTextComponent("block.projectexpansion.emc_link.limit_items", new StringTextComponent(matter.getItemLimitString()).mergeStyle(TextFormatting.GREEN))).mergeStyle(TextFormatting.GRAY));
        list.add((new TranslationTextComponent("block.projectexpansion.emc_link.limit_emc", new StringTextComponent(matter.getLevel() == 16 ? "INFINITY" : EMCFormat.INSTANCE.format(matter.getEMCLimit())).mergeStyle(TextFormatting.GREEN))).mergeStyle(TextFormatting.GRAY));
    }

    @Deprecated
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEMCLink)) return ActionResultType.PASS;
        return ((TileEMCLink) tile).handleActivation(player, hand);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEMCLink) ((TileEMCLink) tile).wasPlaced(livingEntity, stack);
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return false;
    }
}
