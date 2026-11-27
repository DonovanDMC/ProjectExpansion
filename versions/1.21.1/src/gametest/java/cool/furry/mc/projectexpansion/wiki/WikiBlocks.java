package cool.furry.mc.projectexpansion.wiki;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// placing blocks for menus & scenes
public final class WikiBlocks {
    private WikiBlocks() {}

    // shaped to fit its neighbors, like it was placed by a player
    public static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(state, level, pos));
    }

    public static void place(ServerLevel level, BlockPos pos, Block block) {
        place(level, pos, block.defaultBlockState());
    }

    // rows that read left to right from a scene's camera, each row further back
    public static void rows(ServerLevel level, BlockPos pos, List<? extends Block> blocks, int columns) {
        for (int i = 0; i < blocks.size(); i++) place(level, pos.offset(i / columns, 0, i % columns), blocks.get(i));
    }

    // whatever was there before, without its items dropping
    public static void clear(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            level.removeBlockEntity(pos);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }

    // right clicks a block like a player does, with whatever's in their main hand
    // through the game's own handling, so blocks that only handle an empty hand click (useWithoutItem) still get it when the hand isn't empty
    public static void click(ServerPlayer player, BlockPos pos, Direction side) {
        // on the middle of the face, blocks like ae2's cables tell what was clicked from where it was
        player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false));
    }

    // right clicks a block with an empty hand
    public static void use(ServerPlayer player, BlockPos pos, Direction side) {
        player.serverLevel().getBlockState(pos).useWithoutItem(player.serverLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
    }

    public static BlockState face(BlockState state, Direction dir) {
        return state.setValue(DirectionalBlock.FACING, dir);
    }

    public static BlockState face(Block block, Direction dir) {
        return block.defaultBlockState().setValue(DirectionalBlock.FACING, dir);
    }

    public static BlockState faceHorizontal(BlockState state, Direction dir) {
        return state.setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    public static BlockState faceHorizontal(Block block, Direction dir) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir);
    }
}
