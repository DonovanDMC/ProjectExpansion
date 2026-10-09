package cool.furry.mc.neoforge.projectexpansion.integrations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
}
