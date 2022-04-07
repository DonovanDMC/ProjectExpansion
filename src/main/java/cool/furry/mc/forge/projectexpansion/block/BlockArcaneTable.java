/*******************************************************************************************
 **                      Copyright 2020 Sin Tachikawa (sinkillerj)                        **
 **                                                                                       **
 **                     Borrowed From ProjectE Under The MIT License                      **
 **                                                                                       **
 **                    Commit: 4d9657371fc21579ecf87e531711c8d21b7b1185                   **
 **       src/main/java/moze_intel/projecte/gameObjs/blocks/TransmutationStone.java       **
 *******************************************************************************************/
package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.container.ArcaneContainer;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockArcaneTable extends DirectionalBlock implements IWaterLoggable {
    private static final VoxelShape UP_SHAPE = Block.makeCuboidShape(0, 0, 0, 16, 4, 16);
    private static final VoxelShape DOWN_SHAPE = Block.makeCuboidShape(0, 12, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Block.makeCuboidShape(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.makeCuboidShape(0, 0, 0, 16, 16, 4);
    private static final VoxelShape WEST_SHAPE = Block.makeCuboidShape(12, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.makeCuboidShape(0, 0, 0, 4, 16, 16);

    public BlockArcaneTable() {
        super(AbstractBlock.Properties.create(Material.ROCK).setRequiresTool().hardnessAndResistance(10, 30).sound(SoundType.STONE));
        this.setDefaultState(this.getStateContainer().getBaseState().with(FACING, Direction.UP).with(BlockStateProperties.WATERLOGGED, false));
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.WATERLOGGED);
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext ctx) {
        Direction facing = state.get(FACING);
        switch (facing) {
            case DOWN:
                return DOWN_SHAPE;
            case NORTH:
                return NORTH_SHAPE;
            case SOUTH:
                return SOUTH_SHAPE;
            case WEST:
                return WEST_SHAPE;
            case EAST:
                return EAST_SHAPE;
            case UP:
            default:
                return UP_SHAPE;
        }
    }

    @Nonnull
    @Deprecated
    public ActionResultType onBlockActivated(@Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult rtr) {
        if (!world.isRemote) {
            NetworkHooks.openGui((ServerPlayerEntity) player, new ArcaneContainer());
        }

        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.with(FACING, context.getFace()).with(BlockStateProperties.WATERLOGGED, context.getWorld().getFluidState(context.getPos()).getFluid() == Fluids.WATER);
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return state.get(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState updatePostPlacement(@Nonnull BlockState state, @Nonnull Direction facing, @Nonnull BlockState facingState, @Nonnull IWorld world, @Nonnull BlockPos currentPos, @Nonnull BlockPos facingPos) {
        if (state.get(BlockStateProperties.WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }
}