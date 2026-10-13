package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Common;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.EnumProperty;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockEmcPipe extends Block {
    public static final Map<Direction, EnumProperty<PipeSide>> SIDES = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) SIDES.put(side, EnumProperty.create(side.getSerializedName(), PipeSide.class));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
        ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
    }

    public BlockEmcPipe(AbstractBlock.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeSide> property : SIDES.values()) state = state.setValue(property, PipeSide.NONE);
        registerDefaultState(state);
    }

    public static AbstractBlock.Properties getProperties() {
        return AbstractBlock.Properties.of(Material.METAL).strength(0.5F).sound(SoundType.METAL).noOcclusion();
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    /***************
     * Connections *
     ***************/

    private static boolean isDisconnected(IBlockReader level, BlockPos pos, Direction side) {
        TileEntity pipe = level.getBlockEntity(pos);
        return pipe instanceof BlockEntityEmcPipe && ((BlockEntityEmcPipe) pipe).isDisconnected(side);
    }

    private static PipeSide connection(World level, BlockPos pos, BlockState state, Direction side) {
        if (isDisconnected(level, pos, side)) return PipeSide.NONE;
        BlockPos other = pos.relative(side);
        if (level.getBlockState(other).getBlock() instanceof BlockEmcPipe) {
            return isDisconnected(level, other, side.getOpposite()) ? PipeSide.NONE : PipeSide.CONNECTED;
        }
        if (BlockEntityEmcPipe.getEmcStorage(level, other, side.getOpposite()) == null) return PipeSide.NONE;
        // extracting is only ever turned on with a wrench, so keep it while the storage is still there
        return state.hasProperty(SIDES.get(side)) && state.getValue(SIDES.get(side)) == PipeSide.EXTRACTING ? PipeSide.EXTRACTING : PipeSide.CONNECTED;
    }

    public static BlockState computeState(World level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) state = state.setValue(SIDES.get(side), connection(level, pos, state, side));
        return state;
    }

    public static void refresh(World level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockEmcPipe)) return;
        BlockState updated = computeState(level, pos, state);
        if (updated != state) setState(level, pos, updated);
    }

    private static void setState(World level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
        BlockEntityEmcPipe.invalidateNetworks();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockItemUseContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, World level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) BlockEntityEmcPipe.invalidateNetworks();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, World level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide) refresh(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, World level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            TileEntity pipe = level.getBlockEntity(pos);
            if (pipe instanceof BlockEntityEmcPipe) InventoryHelper.dropContents(level, pos, ((BlockEntityEmcPipe) pipe).getUpgrades());
            BlockEntityEmcPipe.invalidateNetworks();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**********
     * VoxelShapes *
     **********/

    private static VoxelShape shape(BlockState state, boolean allSides) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if (allSides || state.getValue(SIDES.get(side)).isConnected()) shape = VoxelShapes.or(shape, ARMS.get(side));
        }
        return shape;
    }

    private static boolean holdingWrench(ISelectionContext context) {
        Entity entity = context.getEntity();
        return entity instanceof PlayerEntity && Wrench.isHoldingWrench((PlayerEntity) entity);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos, ISelectionContext context) {
        // with a wrench every side can be clicked, so disconnected sides can be reconnected
        return shape(state, holdingWrench(context));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, IBlockReader level, BlockPos pos, ISelectionContext context) {
        return shape(state, false);
    }

    // the arm that was clicked, or the face of the core
    public static Direction getClickedSide(BlockPos pos, BlockRayTraceResult hit) {
        Vector3d local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Map.Entry<Direction, VoxelShape> arm : ARMS.entrySet()) {
            if (arm.getValue().bounds().inflate(0.001).contains(local)) return arm.getKey();
        }
        return hit.getDirection();
    }

    /***************
     * Interaction *
     ***************/

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        Direction side = getClickedSide(pos, hit);
        if (Wrench.isWrench(player.getItemInHand(hand))) {
            if (!level.isClientSide) useWrench(level, pos, state, side, player.isShiftKeyDown());
            return ActionResultType.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(SIDES.get(side)) != PipeSide.EXTRACTING) return ActionResultType.PASS;
        if (!level.isClientSide) openMenu(level, pos, side, player);
        return ActionResultType.sidedSuccess(level.isClientSide);
    }

    // sneaking disconnects or reconnects a side, otherwise it switches a side touching a storage between giving and taking
    public static void useWrench(World level, BlockPos pos, BlockState state, Direction side, boolean sneaking) {
        TileEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BlockEntityEmcPipe)) return;
        BlockEntityEmcPipe pipe = (BlockEntityEmcPipe) blockEntity;
        PipeSide current = state.getValue(SIDES.get(side));
        if (sneaking || pipe.isDisconnected(side)) {
            pipe.setDisconnected(side, !pipe.isDisconnected(side));
            refresh(level, pos);
            refresh(level, pos.relative(side));
        } else if (current == PipeSide.CONNECTED && !(level.getBlockState(pos.relative(side)).getBlock() instanceof BlockEmcPipe)) {
            setState(level, pos, state.setValue(SIDES.get(side), PipeSide.EXTRACTING));
        } else if (current == PipeSide.EXTRACTING) {
            setState(level, pos, state.setValue(SIDES.get(side), PipeSide.CONNECTED));
        } else {
            return;
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
    }

    private static void openMenu(World level, BlockPos pos, Direction side, PlayerEntity player) {
        TileEntity blockEntity = level.getBlockEntity(pos);
        if (!(player instanceof ServerPlayerEntity) || !(blockEntity instanceof BlockEntityEmcPipe)) return;
        BlockEntityEmcPipe pipe = (BlockEntityEmcPipe) blockEntity;
        Common.openContainer((ServerPlayerEntity) player, new SimpleNamedContainerProvider((id, inventory, p) -> new ContainerEmcPipe(id, inventory, pipe, side), Lang.GUI.EMC_PIPE.translate()), buf -> {
            buf.writeBlockPos(pos);
            buf.writeEnum(side);
        });
    }

    /*********
     * Misc *
     *********/

    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityEmcPipe();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_PIPE_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.EMC_PIPE_TOOLTIP2.translateColored(TextFormatting.GRAY));
    }
}
