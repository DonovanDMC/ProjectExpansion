package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// backport of minecraft's ContainerOpenersCounter from 1.17
public abstract class ContainerOpenersCounter {
    private int openCount;

    protected abstract void onOpen(World level, BlockPos pos, BlockState state);

    protected abstract void onClose(World level, BlockPos pos, BlockState state);

    protected abstract void openerCountChanged(World level, BlockPos pos, BlockState state, int previous, int count);

    protected abstract boolean isOwnContainer(PlayerEntity player);

    public void incrementOpeners(PlayerEntity player, World level, BlockPos pos, BlockState state) {
        int previous = this.openCount++;
        if (previous == 0) {
            this.onOpen(level, pos, state);
            scheduleRecheck(level, pos, state);
        }

        this.openerCountChanged(level, pos, state, previous, this.openCount);
    }

    public void decrementOpeners(PlayerEntity player, World level, BlockPos pos, BlockState state) {
        int previous = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(level, pos, state);
        }

        this.openerCountChanged(level, pos, state, previous, this.openCount);
    }

    private int getOpenCount(World level, BlockPos pos) {
        AxisAlignedBB box = new AxisAlignedBB(pos.getX() - 5.0, pos.getY() - 5.0, pos.getZ() - 5.0, pos.getX() + 6.0, pos.getY() + 6.0, pos.getZ() + 6.0);
        return level.getEntitiesOfClass(PlayerEntity.class, box, this::isOwnContainer).size();
    }

    public void recheckOpeners(World level, BlockPos pos, BlockState state) {
        int count = this.getOpenCount(level, pos);
        int previous = this.openCount;
        if (previous != count) {
            if (count != 0 && previous == 0) {
                this.onOpen(level, pos, state);
            } else if (count == 0) {
                this.onClose(level, pos, state);
            }

            this.openCount = count;
        }

        this.openerCountChanged(level, pos, state, previous, count);
        if (count > 0) {
            scheduleRecheck(level, pos, state);
        }
    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(World level, BlockPos pos, BlockState state) {
        level.getBlockTicks().scheduleTick(pos, state.getBlock(), 5);
    }
}
