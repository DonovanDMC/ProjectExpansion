package cool.furry.mc.neoforge.projectexpansion.integrations.top;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.integrations.Common;
import cool.furry.mc.neoforge.projectexpansion.integrations.IDataProvider;
import mcjty.theoneprobe.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("unused")
public class ProbeInfoProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {
    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData data) {
        Common.registerCommonTooltips(probeInfo::mcText, new DataProvider(player, level, blockState, data));
    }

    @Override
    public ResourceLocation getID() {
        return Main.rl("plugin");
    }

    @Override
    public Void apply(ITheOneProbe iTheOneProbe) {
        iTheOneProbe.registerProvider(this);
        return null;
    }

    public record DataProvider(Player player, Level level, BlockState state, IProbeHitData data) implements IDataProvider {
        @Override
        public BlockPos getBlockPos() {
            return data.getPos();
        }

        @Override
        public Block getBlock() {
            return state.getBlock();
        }

        @Override
        public BlockState getBlockState() {
            return state;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity() {
            return level.getBlockEntity(data.getPos());
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public Direction getSide() {
            return data.getSideHit();
        }
    }
}