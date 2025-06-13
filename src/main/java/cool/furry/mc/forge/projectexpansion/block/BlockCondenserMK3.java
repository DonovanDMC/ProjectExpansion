package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.util.IChestLike;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.gameObjs.block_entities.EmcChestBlockEntity;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockCondenserMK3 extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    public BlockCondenserMK3() {
        super(Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(10, 3_600_000));
        this.registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.CONDENSER_MK3_TOOLTIP2.translateColored(ChatFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override
    @Deprecated
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @Deprecated
    protected InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult rtr) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntityCondenserMK3 chest = WorldHelper.getBlockEntity(BlockEntityCondenserMK3.class, level, pos, true);
        if (chest != null) {
            player.openMenu(chest, pos);
            player.awardStat(Stats.OPEN_CHEST);
            PiglinAi.angerNearbyPiglins(player, true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @Deprecated
    public boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        return triggerBlockEntityEvent(state, level, pos, id, param);
    }

    public boolean triggerBlockEntityEvent(@NotNull BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity blockEntity = WorldHelper.getBlockEntity(BlockEntityCondenserMK3.class, level, pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    @Override
    @Deprecated
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        EmcChestBlockEntity chest = WorldHelper.getBlockEntity(EmcChestBlockEntity.class, level, pos);
        if (chest != null) {
            chest.recheckOpen();
        }
    }

    @Override
    @Deprecated
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return ItemHandlerHelper.calcRedstoneFromInventory(WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, null, null));
    }

    @NotNull
    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @NotNull
    @Override
    @Deprecated
    public BlockState updateShape(@NotNull BlockState state, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor level,
                                  @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    @Deprecated
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            for (Direction direction : Direction.values()) {
                IItemHandler handler = WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, null, direction);
                WorldHelper.dropInventory(handler, level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    @Deprecated
    public void attack(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player) {
        if (!level.isClientSide) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.is(PEItems.PHILOSOPHERS_STONE)) {
                level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FACING, player.getDirection().getOpposite()));
            }
        }
    }

    @Deprecated
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Deprecated
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntityCondenserMK3(blockPos, blockState);
    }

    @javax.annotation.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.CONDENSER_MK3.get()) {
            if (level.isClientSide) return BlockEntityCondenserMK3::tickClient;
            else return BlockEntityCondenserMK3::tickServer;
        }
        return null;
    }
}
