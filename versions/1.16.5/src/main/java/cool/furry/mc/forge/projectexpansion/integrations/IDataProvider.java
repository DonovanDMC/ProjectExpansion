package cool.furry.mc.forge.projectexpansion.integrations;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface IDataProvider {
    BlockPos getBlockPos();
    Block getBlock();
    BlockState getBlockState();
    @Nullable TileEntity getBlockEntity();
    World getLevel();
    @SuppressWarnings("unused")
    PlayerEntity getPlayer();
    Direction getSide();
}
