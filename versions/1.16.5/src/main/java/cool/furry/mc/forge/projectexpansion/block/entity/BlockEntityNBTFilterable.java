package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.TextFormatting;

public class BlockEntityNBTFilterable extends BlockEntityOwnable {
    public static final BooleanProperty FILTER = BooleanProperty.create("filter");
    public BlockEntityNBTFilterable(TileEntityType<?> type) {
        super(type);
    }

    public void toggleFilter(PlayerEntity player) {
        if(level == null || level.isClientSide || !this.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return;
        boolean status = getFilterStatus();
        if(status) {
            setFilterStatus(false);
            player.displayClientMessage(Lang.NBT_FILTER_DISABLED.translateColored(TextFormatting.RED), true);
        } else {
            setFilterStatus(true);
            player.displayClientMessage(Lang.NBT_FILTER_ENABLED.translateColored(TextFormatting.GREEN), true);
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
 