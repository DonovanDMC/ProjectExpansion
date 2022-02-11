package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockPowerFlower extends Block {
    public static final VoxelShape SHAPE = VoxelShapes.or(
            makeCuboidShape(0, 0, 0, 16, 1, 16),
            makeCuboidShape(3.5, 4, 6.5, 12.5, 13, 9.5),
            makeCuboidShape(6.5, 1, 6.5, 9.5, 16, 9.5),
            makeCuboidShape(6.5, 4, 3.5, 9.5, 13, 12.5),
            makeCuboidShape(6.5, 7, 0.5, 9.5, 10, 15.5),
            makeCuboidShape(3.5, 7, 3.5, 12.5, 10, 12.5),
            makeCuboidShape(0.5, 7, 6.5, 15.5, 10, 9.5)
    );
    public final Matter matter;

    public BlockPowerFlower(Matter matter) {
        super(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(1F));
        this.matter = matter;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TilePowerFlower();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext ctx) {
        return SHAPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add(new TranslationTextComponent("block.projectexpansion.power_flower.tooltip", new StringTextComponent(Config.tickDelay.get().toString()).mergeStyle(TextFormatting.GREEN), new StringTextComponent(Config.tickDelay.get() == 1 ? "" : "s").mergeStyle(TextFormatting.GRAY)).mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("block.projectexpansion.power_flower.emc", new StringTextComponent("").appendSibling(TransmutationEMCFormatter.formatEMC(matter.getPowerFlowerOutput())).mergeStyle(TextFormatting.GREEN)).mergeStyle(TextFormatting.GRAY));
    }

    @Deprecated
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TilePowerFlower) player.sendStatusMessage(new StringTextComponent(((TilePowerFlower) tile).ownerName), true);
        return super.onBlockActivated(state, world, pos, player, hand, ray);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TilePowerFlower) ((TilePowerFlower) tile).wasPlaced(livingEntity, stack);
    }
}
