package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockTypes;
import cool.furry.mc.neoforge.projectexpansion.util.IHasColor;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

// Many methods lovingly borrowed (stolen) from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/mc1.18.x/src/main/java/moze_intel/projecte/gameObjs/blocks/AlchemicalChest.java
public class BlockAdvancedAlchemicalChest extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock, IHasColor {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	private final DyeColor color;

	public BlockAdvancedAlchemicalChest(DyeColor color) {
		super(Block.Properties.of().requiresCorrectToolForDrops().strength(10, 3_600_000).lightLevel((state) -> 10));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.FALSE));
		this.color = color;
	}

	@NotNull
	@Override
	public DyeColor getColor() {
		return color;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, list, tooltipFlag);
		list.add(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_TOOLTIP.translateColored(ChatFormatting.GRAY));
		list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
	}

	@Override
    public RenderShape getRenderShape(BlockState p_51567_) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityAdvancedAlchemicalChest(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return InteractionResult.FAIL;

		InteractionHand hand = player.getUsedItemHand();
		player.openMenu(new ContainerProvider(blockEntity, hand), (buf) -> {
			buf.writeEnum(hand);
			buf.writeByte(player.getInventory().selected);
			buf.writeBoolean(false);
			buf.writeBlockPos(pos);
		});
		player.awardStat(Stats.OPEN_CHEST);
		PiglinAi.angerNearbyPiglins(player, true);

		return InteractionResult.CONSUME;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return ItemInteractionResult.SUCCESS;
		}

		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return ItemInteractionResult.FAIL;
		if (player.isCrouching()) {
			PiglinAi.angerNearbyPiglins(player, true);
			return blockEntity.handleItemActivation(player, stack);
		}

		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return;
		blockEntity.handlePlace(livingEntity, stack);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type == BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get() && level.isClientSide) return BlockEntityAdvancedAlchemicalChest::tickClient;
		return null;
	}

	@Override
	public VoxelShape getShape(BlockState p_51569_, BlockGetter p_51570_, BlockPos p_51571_, CollisionContext p_51572_) {
		return SHAPE;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) be.recheckOpen();
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) {
			return super.getAnalogOutputSignal(state, level, pos);
		}

		IItemHandler handler = WorldHelper.getCapability(level, Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, Direction.UP);
		if (handler == null) {
			return super.getAnalogOutputSignal(state, level, pos);
		}

		return ItemHandlerHelper.calcRedstoneFromInventory(handler);
	}

	@Nonnull
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING, context.getPlayer() == null ? Direction.NORTH : context.getPlayer().getDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Nonnull
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Nonnull
	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity blockEntity = WorldHelper.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		return blockEntity != null && blockEntity.triggerEvent(id, param);
	}

	@Override
	public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
		return MapColor.byId(this.color.getId());
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return BlockTypes.ADVANCED_ALCHEMICAL_CHEST.get();
	}

	// graciously borrowed from ProjectE
	// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/gameObjs/items/AlchemicalBag.java#L76-L100
	private record ContainerProvider(BlockEntityAdvancedAlchemicalChest blockEntity, InteractionHand hand) implements MenuProvider {
		@Nonnull
		@Override
		public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
			IItemHandlerModifiable inv = (IItemHandlerModifiable) blockEntity.getBag();
			return new ContainerAdvancedAlchemicalChest(windowId, playerInventory, hand, Objects.requireNonNull(inv), playerInventory.selected, false, blockEntity);
		}

		@Nonnull
		@Override
		public Component getDisplayName() {
			return Lang.ADVANCED_ALCHEMICAL_CHEST_TITLE.translate();
		}
	}
}
