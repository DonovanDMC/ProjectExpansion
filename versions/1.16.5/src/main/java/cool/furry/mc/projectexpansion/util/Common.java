package cool.furry.mc.projectexpansion.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

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

    public static ActionResultType use(@Nullable FiveFunction<BlockState, World, BlockPos, PlayerEntity, BlockRayTraceResult, ActionResultType> withoutItem, @Nullable SevenFunction<ItemStack, BlockState, World, BlockPos, PlayerEntity, Hand, BlockRayTraceResult, ActionResultType> withItem, BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.isEmpty() && withoutItem != null) return withoutItem.apply(state, level, pos, player, hit);
        else if (withItem != null) return withItem.apply(stack, state, level, pos, player, hand, hit);
        return ActionResultType.PASS;
    }

    public static void openContainer(ServerPlayerEntity player, INamedContainerProvider provider, Consumer<PacketBuffer> extraData) {
        NetworkHooks.openGui(player, provider, extraData);
    }

    public static void openContainer(ServerPlayerEntity player, INamedContainerProvider provider, BlockPos pos) {
        openContainer(player, provider, (buf) -> buf.writeBlockPos(pos));
    }

    public static void openContainer(ServerPlayerEntity player, INamedContainerProvider provider) {
        player.openMenu(provider);
    }

    public static ItemStack loadItemStackOptional(CompoundNBT tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    public static CompoundNBT saveItemStackOptional(ItemStack stack) {
        return stack.isEmpty() ? new CompoundNBT() : stack.save(new CompoundNBT());
    }
}