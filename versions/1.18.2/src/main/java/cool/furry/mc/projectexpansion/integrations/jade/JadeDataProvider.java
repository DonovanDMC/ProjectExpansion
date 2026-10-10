package cool.furry.mc.projectexpansion.integrations.jade;

import cool.furry.mc.projectexpansion.integrations.Common;
import cool.furry.mc.projectexpansion.integrations.EmcServerData;
import cool.furry.mc.projectexpansion.integrations.IDataProvider;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.config.IPluginConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class JadeDataProvider implements IComponentProvider, IServerDataProvider<BlockEntity> {
    static final JadeDataProvider INSTANCE = new JadeDataProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, new DataProvider(accessor));
    }

    // jade doesn't say which side is being looked at here
    @Override
    public void appendServerData(CompoundTag data, ServerPlayer player, Level level, BlockEntity blockEntity, boolean showDetails) {
        EmcServerData.append(data, level, blockEntity.getBlockPos(), blockEntity, null);
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

        @Override
        public CompoundTag getServerData() {
            return accessor.getServerData();
        }
    }
}
