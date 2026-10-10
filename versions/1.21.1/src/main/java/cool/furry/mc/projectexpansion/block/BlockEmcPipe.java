package cool.furry.mc.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityEmcPipe;
import cool.furry.mc.projectexpansion.gui.container.ContainerEmcPipe;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.registries.BlockTypes;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.pipe.PipeSide;
import cool.furry.mc.projectexpansion.util.pipe.Wrench;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BlockEmcPipe extends Block implements EntityBlock {
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

    public BlockEmcPipe(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeSide> property : SIDES.values()) state = state.setValue(property, PipeSide.NONE);
        registerDefaultState(state);
    }

    public static BlockBehaviour.Properties getProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.METAL).noOcclusion();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockTypes.EMC_PIPE.get();
    }

    /***************
     * Connections *
     ***************/

    private static boolean isDisconnected(BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof BlockEntityEmcPipe pipe && pipe.isDisconnected(side);
    }

    private static PipeSide connection(Level level, BlockPos pos, BlockState state, Direction side) {
        if (isDisconnected(level, pos, side)) return PipeSide.NONE;
        BlockPos other = pos.relative(side);
        if (level.getBlockState(other).getBlock() instanceof BlockEmcPipe) {
            return isDisconnected(level, other, side.getOpposite()) ? PipeSide.NONE : PipeSide.CONNECTED;
        }
        if (level.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, other, side.getOpposite()) == null) return PipeSide.NONE;
        // extracting is only ever turned on with a wrench, so keep it while the storage is still there
        return state.hasProperty(SIDES.get(side)) && state.getValue(SIDES.get(side)) == PipeSide.EXTRACTING ? PipeSide.EXTRACTING : PipeSide.CONNECTED;
    }

    public static BlockState computeState(Level level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) state = state.setValue(SIDES.get(side), connection(level, pos, state, side));
        return state;
    }

    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockEmcPipe)) return;
        BlockState updated = computeState(level, pos, state);
        if (updated != state) setState(level, pos, updated);
    }

    private static void setState(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        // the extracting sides expose a capability, and the network has changed
        level.invalidateCapabilities(pos);
        BlockEntityEmcPipe.invalidateNetworks();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) BlockEntityEmcPipe.invalidateNetworks();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide) refresh(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BlockEntityEmcPipe pipe) Containers.dropContents(level, pos, pipe.getUpgrades());
            BlockEntityEmcPipe.invalidateNetworks();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**********
     * Shapes *
     **********/

    private static VoxelShape shape(BlockState state, boolean allSides) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if (allSides || state.getValue(SIDES.get(side)).isConnected()) shape = Shapes.or(shape, ARMS.get(side));
        }
        return shape;
    }

    private static boolean holdingWrench(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player && Wrench.isHoldingWrench(player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // with a wrench every side can be clicked, so disconnected sides can be reconnected
        return shape(state, holdingWrench(context));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state, false);
    }

    // the arm that was clicked, or the face of the core
    public static Direction getClickedSide(BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Map.Entry<Direction, VoxelShape> arm : ARMS.entrySet()) {
            if (arm.getValue().bounds().inflate(0.001).contains(local)) return arm.getKey();
        }
        return hit.getDirection();
    }

    /***************
     * Interaction *
     ***************/

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Direction side = getClickedSide(pos, hit);
        if (Wrench.isWrench(stack)) {
            if (!level.isClientSide) useWrench(level, pos, state, side, player.isShiftKeyDown());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(SIDES.get(side)) == PipeSide.EXTRACTING) {
            if (!level.isClientSide) openMenu(level, pos, side, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction side = getClickedSide(pos, hit);
        if (state.getValue(SIDES.get(side)) != PipeSide.EXTRACTING) return InteractionResult.PASS;
        if (!level.isClientSide) openMenu(level, pos, side, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // sneaking disconnects or reconnects a side, otherwise it switches a side touching a storage between giving and taking
    public static void useWrench(Level level, BlockPos pos, BlockState state, Direction side, boolean sneaking) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityEmcPipe pipe)) return;
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
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private static void openMenu(Level level, BlockPos pos, Direction side, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof BlockEntityEmcPipe pipe)) return;
        serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ContainerEmcPipe(id, inventory, pipe, side), Lang.GUI.EMC_PIPE.translate()), buf -> {
            buf.writeBlockPos(pos);
            buf.writeEnum(side);
        });
    }

    /*********
     * Misc *
     *********/

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityEmcPipe(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.EMC_PIPE.get() && !level.isClientSide) return BlockEntityEmcPipe::tickServer;
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, context, list, flag);
        list.add(Lang.Blocks.EMC_PIPE_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.EMC_PIPE_TOOLTIP2.translateColored(ChatFormatting.GRAY));
    }
}
