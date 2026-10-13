package cool.furry.mc.projectexpansion.integrations.jade;

import cool.furry.mc.projectexpansion.integrations.Common;
import cool.furry.mc.projectexpansion.integrations.EmcServerData;
import cool.furry.mc.projectexpansion.integrations.IDataProvider;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerDataProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JadeDataProvider implements IComponentProvider, IServerDataProvider<TileEntity> {
    static final JadeDataProvider INSTANCE = new JadeDataProvider();

    @Override
    public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, new DataProvider(accessor));
    }

    // the side being looked at isn't given here
    @Override
    public void appendServerData(CompoundNBT data, ServerPlayerEntity player, World level, TileEntity blockEntity) {
        EmcServerData.append(data, level, blockEntity.getBlockPos(), blockEntity, null);
    }

    public static class DataProvider implements IDataProvider {
        private final IDataAccessor accessor;

        public DataProvider(IDataAccessor accessor) {
            this.accessor = accessor;
        }

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
        public @Nullable TileEntity getBlockEntity() {
            return accessor.getTileEntity();
        }

        @Override
        public World getLevel() {
            return accessor.getWorld();
        }

        @Override
        public PlayerEntity getPlayer() {
            return accessor.getPlayer();
        }

        @Override
        public Direction getSide() {
            return accessor.getSide();
        }

        @Override
        public CompoundNBT getServerData() {
            return accessor.getServerData();
        }
    }
}
