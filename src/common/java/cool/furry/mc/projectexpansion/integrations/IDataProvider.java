package cool.furry.mc.projectexpansion.integrations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IDataProvider {
    BlockPos getBlockPos();
    Block getBlock();
    BlockState getBlockState();
    @Nullable BlockEntity getBlockEntity();
    Level getLevel();
    @SuppressWarnings("unused")
    Player getPlayer();
    Direction getSide();
    // values the server sent along, for anything the client copy of the block doesn't keep up to date
    default @Nullable CompoundTag getServerData() {
        return null;
    }
}
