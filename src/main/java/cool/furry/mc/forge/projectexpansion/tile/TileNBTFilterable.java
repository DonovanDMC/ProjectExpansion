package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TileNBTFilterable extends TileOwnable {
    public static final BooleanProperty FILTER = BooleanProperty.create("filter");
    public TileNBTFilterable(TileEntityType<?> tileEntityType) {
        super(tileEntityType);
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
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(FILTER, status));
    }
}
