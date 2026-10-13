package cool.furry.mc.projectexpansion.integrations.top;

import cool.furry.mc.projectexpansion.Main;
import cool.furry.mc.projectexpansion.integrations.Common;
import cool.furry.mc.projectexpansion.integrations.IDataProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("unused")
public class ProbeInfoProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {
    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World level, BlockState blockState, IProbeHitData data) {
        Common.registerCommonTooltips(probeInfo::mcText, new DataProvider(player, level, blockState, data));
    }

    @Override
    public String getID() {
        return Main.rl("plugin").toString();
    }

    @Override
    public Void apply(ITheOneProbe iTheOneProbe) {
        iTheOneProbe.registerProvider(this);
        return null;
    }

    public static class DataProvider implements IDataProvider {
        private final PlayerEntity player;
        private final World level;
        private final BlockState state;
        private final IProbeHitData data;

        public DataProvider(PlayerEntity player, World level, BlockState state, IProbeHitData data) {
            this.player = player;
            this.level = level;
            this.state = state;
            this.data = data;
        }

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
        public @Nullable TileEntity getBlockEntity() {
            return level.getBlockEntity(data.getPos());
        }

        @Override
        public World getLevel() {
            return level;
        }

        @Override
        public PlayerEntity getPlayer() {
            return player;
        }

        @Override
        public Direction getSide() {
            return data.getSideHit();
        }
    }
}
