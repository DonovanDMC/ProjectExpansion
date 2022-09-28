package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class BlockEntityNBTFilterable extends BlockEntityOwnable {
    public static final BooleanProperty FILTER = BooleanProperty.create("filter");
    public BlockEntityNBTFilterable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void toggleFilter(Player player) {
        if(level == null || level.isClientSide || !this.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return;
        boolean status = getFilterStatus();
        if(status) {
            setFilterStatus(false);
            player.displayClientMessage(Component.translatable("text.projectexpansion.nbt_filter.disable").setStyle(ColorStyle.RED), true);
        } else {
            setFilterStatus(true);
            player.displayClientMessage(Component.translatable("text.projectexpansion.nbt_filter.enable").setStyle(ColorStyle.GREEN), true);
        }
    }

    public boolean getFilterStatus() {
        return getBlockState().getValue(FILTER);
    }

    public void setFilterStatus(boolean status) {
        if(level == null || level.isClientSide) return;
        level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(FILTER, status));
    }
}
 