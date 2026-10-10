package cool.furry.mc.projectexpansion.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

// mirrors the 1.20.5+ split of Block#use, blocks still need to override use and call IUsableBlock.super.use
public interface IUsableBlock {
    default ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            ActionResultType result = useItemOn(stack, state, level, pos, player, hand, hit);
            if (result != ActionResultType.PASS) return result;
        }
        return hand == Hand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : ActionResultType.PASS;
    }

    default ActionResultType useWithoutItem(BlockState state, World level, BlockPos pos, PlayerEntity player, BlockRayTraceResult hitResult) {
        return ActionResultType.PASS;
    }

    default ActionResultType useItemOn(ItemStack stack, BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hitResult) {
        return ActionResultType.PASS;
    }
}
