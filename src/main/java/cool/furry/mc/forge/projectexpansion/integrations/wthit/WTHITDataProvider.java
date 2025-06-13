package cool.furry.mc.neoforge.projectexpansion.integrations.wthit;

import cool.furry.mc.neoforge.projectexpansion.integrations.Common;
import cool.furry.mc.neoforge.projectexpansion.integrations.IDataProvider;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WTHITDataProvider implements IBlockComponentProvider {

    static final WTHITDataProvider INSTANCE = new WTHITDataProvider();

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::addLine, new DataProvider(accessor));
    }

    public record DataProvider(IBlockAccessor accessor) implements IDataProvider {
        @Override
        public BlockPos getBlockPos() {
            return accessor.getPosition();
        }

        @Override
        public Block getBlock() {
            return accessor.getBlock();
        }

        @Override
        public BlockState getBlockState() {
            return accessor.getBlockState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity() {
            return accessor.getBlockEntity();
        }

        @Override
        public Level getLevel() {
            return accessor.getWorld();
        }

        @Override
        public Player getPlayer() {
            return accessor.getPlayer();
        }

        @Override
        public Direction getSide() {
            return accessor.getSide();
        }
    }
}
