package cool.furry.mc.forge.projectexpansion.integrations.jade;

import cool.furry.mc.forge.projectexpansion.integrations.Common;
import cool.furry.mc.forge.projectexpansion.integrations.IDataProvider;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.config.IPluginConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class JadeDataProvider implements IComponentProvider {
    static final JadeDataProvider INSTANCE = new JadeDataProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, new DataProvider(accessor));
    }

    public record DataProvider(BlockAccessor accessor) implements IDataProvider {
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
            return accessor.getLevel();
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
