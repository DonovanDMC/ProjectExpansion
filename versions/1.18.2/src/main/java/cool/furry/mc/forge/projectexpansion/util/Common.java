package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class Common {
    @FunctionalInterface
    public interface FiveFunction<P1, P2, P3, P4, P5, R> {
        R apply(P1 arg1, P2 arg2, P3 arg3, P4 arg4, P5 arg5);
    }

    @FunctionalInterface
    public interface SevenFunction<P1, P2, P3, P4, P5, P6, P7, R> {
        R apply(P1 arg1, P2 arg2, P3 arg3, P4 arg4, P5 arg5, P6 arg6, P7 arg7);
    }

    public static InteractionResult use(@Nullable FiveFunction<BlockState, Level, BlockPos, Player, BlockHitResult, InteractionResult> withoutItem, @Nullable SevenFunction<ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult, InteractionResult> withItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.isEmpty() && withoutItem != null) return withoutItem.apply(state, level, pos, player, hit);
        else if (withItem != null) return withItem.apply(stack, state, level, pos, player, hand, hit);
        return InteractionResult.PASS;
    }

    public static void openContainer(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        NetworkHooks.openGui(player, provider, extraData);
    }

    public static void openContainer(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        openContainer(player, provider, (buf) -> buf.writeBlockPos(pos));
    }

    public static void openContainer(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }

    public static ItemStack loadItemStackOptional(CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    public static CompoundTag saveItemStackOptional(ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }
}