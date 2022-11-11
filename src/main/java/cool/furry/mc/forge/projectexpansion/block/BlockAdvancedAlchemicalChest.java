package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.tile.TileAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.tile.TileOwnable;
import cool.furry.mc.forge.projectexpansion.util.AdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasColor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.piglin.PiglinTasks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

// Many methods lovingly borrowed (stolen) from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/mc1.18.x/src/main/java/moze_intel/projecte/gameObjs/blocks/AlchemicalChest.java
public class BlockAdvancedAlchemicalChest extends HorizontalBlock implements IWaterLoggable, IHasColor {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	private final DyeColor color;

	public BlockAdvancedAlchemicalChest(DyeColor color) {
		super(Block.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(10, 3_600_000).lightLevel((state) -> 10));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.FALSE));
		this.color = color;
	}

	@Nonnull
	@Override
	public DyeColor getColor() {
		return color;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(new TranslationTextComponent("block.projectexpansion.advanced_alchemical_chest.tooltip").setStyle(ColorStyle.GRAY));
		list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new TileAdvancedAlchemicalChest(AdvancedAlchemicalChest.getBlockEntityType(color), color);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if (level.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		TileEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof TileAdvancedAlchemicalChest) {
			TileAdvancedAlchemicalChest be = (TileAdvancedAlchemicalChest) blockEntity;
			if(player.isCrouching()) {
				PiglinTasks.angerNearbyPiglins(player, true);
				return be.handleActivation(player, hand);
			} else {
				if(be.handleActivation(player, TileOwnable.ActivationType.CHECK_OWNERSHIP)) {
					NetworkHooks.openGui((ServerPlayerEntity) player, new ContainerProvider(be, hand), (buf) -> {
						buf.writeEnum(hand);
						buf.writeByte(player.inventory.selected);
						buf.writeBoolean(false);
						buf.writeBlockPos(pos);
					});
					player.awardStat(Stats.OPEN_CHEST);
					PiglinTasks.angerNearbyPiglins(player, true);
					return ActionResultType.CONSUME;
				}
			}
		}

		return ActionResultType.FAIL;
	}

	@Override
	public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
		TileEntity tile = level.getBlockEntity(pos);
		if (tile instanceof TileAdvancedAlchemicalChest) ((TileAdvancedAlchemicalChest) tile).handlePlace(livingEntity, stack);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState p_220053_1_, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockRenderType getRenderShape(BlockState p_149645_1_) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
		TileEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof TileAdvancedAlchemicalChest) {
			return blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).map(ItemHandlerHelper::calcRedstoneFromInventory).orElse(0);
		}
		return 0;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
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
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	// graciously borrowed from ProjectE
	// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/gameObjs/items/AlchemicalBag.java#L76-L100
	private static class ContainerProvider implements INamedContainerProvider {
		private final TileAdvancedAlchemicalChest blockEntity;
		private final Hand hand;

		private ContainerProvider(TileAdvancedAlchemicalChest blockEntity, Hand hand) {
			this.blockEntity = blockEntity;
			this.hand = hand;
		}

		@Nonnull
		@Override
		public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
			IItemHandlerModifiable inv = (IItemHandlerModifiable) blockEntity.getBag();
			return new ContainerAdvancedAlchemicalChest(windowId, playerInventory, hand, Objects.requireNonNull(inv), playerInventory.selected, false, blockEntity);
		}

		@Nonnull
		@Override
		public ITextComponent getDisplayName() {
			return new TranslationTextComponent("gui.projectexpansion.advanced_alchemical_chest.title");
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean triggerEvent(BlockState state, World level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		TileEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(id, param);
	}

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}
}
