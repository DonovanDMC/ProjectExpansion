package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerAdvancedAlchemicalChest;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.pathfinding.PathType;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// Many methods lovingly borrowed (stolen) from ProjectE
// https://github.com/sinkillerj/ProjectE/blob/mc1.18.x/src/main/java/moze_intel/projecte/gameObjs/blocks/AlchemicalChest.java
public class BlockAdvancedAlchemicalChest extends HorizontalBlock implements IWaterLoggable, IHasColor, IUsableBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
	private final DyeColor color;

	public BlockAdvancedAlchemicalChest(AbstractBlock.Properties properties, DyeColor color) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
		this.color = color;
	}

	// map colors can only be set through properties before 1.17
	public static AbstractBlock.Properties getProperties(DyeColor color) {
		return AbstractBlock.Properties.of(Material.STONE, color.getMaterialColor()).requiresCorrectToolForDrops().strength(10, 3_600_000).lightLevel((state) -> 10);
	}

	@Override
	public DyeColor getColor() {
		return color;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(Lang.Blocks.ADVANCED_ALCHEMICAL_CHEST_TOOLTIP.translateColored(TextFormatting.GRAY));
		list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
	}

	@Override
	@SuppressWarnings("deprecation")
    public BlockRenderType getRenderShape(BlockState p_51567_) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState p_51569_, IBlockReader p_51570_, BlockPos p_51571_, ISelectionContext p_51572_) {
		return SHAPE;
	}

	@Nullable
	// block entities are created through forge's hooks before 1.17
	@Override
	public boolean hasTileEntity(BlockState state) {
	    return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader level) {
	    return new BlockEntityAdvancedAlchemicalChest();
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		return IUsableBlock.super.use(state, level, pos, player, hand, hit);
	}


	@Override
	public ActionResultType useWithoutItem(BlockState state, World level, BlockPos pos, PlayerEntity player, BlockRayTraceResult hitResult) {
		if (level.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getTileEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return ActionResultType.FAIL;

		Hand hand = player.getUsedItemHand();
		Common.openContainer((ServerPlayerEntity) player, new ContainerProvider(blockEntity, hand), (buf) -> {
			buf.writeEnum(hand);
			buf.writeByte(player.inventory.selected);
			buf.writeBoolean(false);
			buf.writeBlockPos(pos);
		});
		player.awardStat(Stats.OPEN_CHEST);
		PiglinTasks.angerNearbyPiglins(player, true);

		return ActionResultType.CONSUME;
	}

	@Override
	public ActionResultType useItemOn(ItemStack stack, BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hitResult) {
		if (level.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getTileEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return ActionResultType.FAIL;
		if (player.isCrouching()) {
			PiglinTasks.angerNearbyPiglins(player, true);
			return blockEntity.handleItemActivation(player, stack);
		}

		return ActionResultType.PASS;
	}

	@Override
	public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
		BlockEntityAdvancedAlchemicalChest blockEntity = WorldHelper.getTileEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		if (blockEntity == null) return;
		blockEntity.handlePlace(livingEntity, stack);
	}

	@Nullable

	@Override
	@SuppressWarnings("deprecation")
	public void tick(BlockState state, ServerWorld level, BlockPos pos, Random random) {
		TileEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockEntityAdvancedAlchemicalChest) ((BlockEntityAdvancedAlchemicalChest) blockEntity).recheckOpen();
	}

	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState state, IBlockReader level, BlockPos blockPos, PathType pathComputationType) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
		return Util.getBlockEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos)
				.map(blockEntity -> blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).map(ItemHandlerHelper::calcRedstoneFromInventory).orElse(0))
				.orElse(0);
	}


	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld level, BlockPos currentPos, BlockPos facingPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean triggerEvent(BlockState state, World level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		TileEntity blockEntity = WorldHelper.getTileEntity(BlockEntityAdvancedAlchemicalChest.class, level, pos);
		return blockEntity != null && blockEntity.triggerEvent(id, param);
	}


	// graciously borrowed from ProjectE
	// https://github.com/sinkillerj/ProjectE/blob/98aee771bdb09beecf51b5608938d93de6f1afb6/src/main/java/moze_intel/projecte/gameObjs/items/AlchemicalBag.java#L76-L100
	private static final class ContainerProvider implements INamedContainerProvider {
	    private final BlockEntityAdvancedAlchemicalChest blockEntity;
	    private final Hand hand;

	    public ContainerProvider(BlockEntityAdvancedAlchemicalChest blockEntity, Hand hand) {
	        this.blockEntity = blockEntity;
	        this.hand = hand;
	    }

	    public BlockEntityAdvancedAlchemicalChest blockEntity() {
	        return blockEntity;
	    }

	    public Hand hand() {
	        return hand;
	    }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
	        ContainerProvider that = (ContainerProvider) o;
	        return Objects.equals(blockEntity, that.blockEntity) && Objects.equals(hand, that.hand);
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(blockEntity, hand);
	    }

	    @Override
	    public String toString() {
	        return "ContainerProvider[blockEntity=" + blockEntity + ", hand=" + hand + "]";
	    }

		@Override
		public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
			IItemHandlerModifiable inv = (IItemHandlerModifiable) blockEntity.getBag();
            if (inv == null) throw new NullPointerException("Bag is null");
			return new ContainerAdvancedAlchemicalChest(windowId, playerInventory, hand, inv, playerInventory.selected, false, blockEntity);
		}

		@Override
		public ITextComponent getDisplayName() {
			return Lang.ADVANCED_ALCHEMICAL_CHEST_TITLE.translate();
		}
	
	}
}
