package cool.furry.mc.neoforge.projectexpansion.integrations.jade;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.integrations.Common;
import cool.furry.mc.neoforge.projectexpansion.integrations.IDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class JadeDataProvider implements IBlockComponentProvider {
    static final JadeDataProvider INSTANCE = new JadeDataProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, new DataProvider(accessor));
    }

    @Override
    public ResourceLocation getUid() {
        return Main.rl("provider");
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
