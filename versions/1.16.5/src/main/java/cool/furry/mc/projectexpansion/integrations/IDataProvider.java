package cool.furry.mc.projectexpansion.integrations;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
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
    PlayerEntity getPlayer();
    Direction getSide();
    // values the server sent along, for anything the client copy of the block doesn't keep up to date
    default @Nullable CompoundNBT getServerData() {
        return null;
    }
}
