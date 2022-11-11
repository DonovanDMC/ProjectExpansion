package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
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
		super(Block.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(10, 3_600_000).lightLevel((state) -> 10));
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
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(Component.translatable("block.projectexpansion.advanced_alchemical_chest.tooltip").setStyle(ColorStyle.GRAY));
		list.add(Component.translatable("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
	}

	@Override
	@SuppressWarnings("deprecation")
	public RenderShape getRenderShape(BlockState p_51567_) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntityAdvancedAlchemicalChest(pos, state, AdvancedAlchemicalChest.getBlockEntityType(color), color);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) {
			if(player.isCrouching()) {
				PiglinAi.angerNearbyPiglins(player, true);
				return be.handleActivation(player, hand);
			} else {
				if(be.handleActivation(player, BlockEntityOwnable.ActivationType.CHECK_OWNERSHIP)) {
					NetworkHooks.openScreen((ServerPlayer) player, new ContainerProvider(be, hand), (buf) -> {
						buf.writeEnum(hand);
						buf.writeByte(player.getInventory().selected);
						buf.writeBoolean(false);
						buf.writeBlockPos(pos);
					});
					player.awardStat(Stats.OPEN_CHEST);
					PiglinAi.angerNearbyPiglins(player, true);
					return InteractionResult.CONSUME;
				}
			}
		}

		return InteractionResult.FAIL;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) be.handlePlace(livingEntity, stack);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type == AdvancedAlchemicalChest.getBlockEntityType(color) && level.isClientSide) return BlockEntityAdvancedAlchemicalChest::tickClient;
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState p_51569_, BlockGetter p_51570_, BlockPos p_51571_, CollisionContext p_51572_) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) be.recheckOpen();
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState p_51522_, BlockGetter p_51523_, BlockPos p_51524_, PathComputationType p_51525_) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest be) {
			return be.getCapability(ForgeCapabilities.ITEM_HANDLER).map(ItemHandlerHelper::calcRedstoneFromInventory).orElse(0);
		}
		return 0;
	}

	@Nonnull
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING, context.getPlayer() == null ? Direction.NORTH : context.getPlayer().getDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
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
				return Component.translatable("gui.projectexpansion.advanced_alchemical_chest.title");
			}
		}

	@Override
	@SuppressWarnings("deprecation")
	public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(id, param);
	}
}
