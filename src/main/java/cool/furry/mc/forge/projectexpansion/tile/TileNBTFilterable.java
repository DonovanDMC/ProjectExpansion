package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.TranslationTextComponent;

public class TileNBTFilterable extends TileOwnable {
    public static final BooleanProperty FILTER = BooleanProperty.create("filter");

    public TileNBTFilterable(TileEntityType<?> tileEntity) {
        super(tileEntity);
    }

    public void toggleFilter(PlayerEntity player) {
        if(world == null || world.isRemote || !this.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return;
        boolean status = getFilterStatus();
        if(status) {
            setFilterStatus(false);
            player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.nbt_filter.disable").setStyle(ColorStyle.RED), true);
        } else {
            setFilterStatus(true);
            player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.nbt_filter.enable").setStyle(ColorStyle.GREEN), true);
        }
    }

    public boolean getFilterStatus() {
        return getBlockState().get(FILTER);
    }

    public void setFilterStatus(boolean status) {
        if(world == null || world.isRemote) return;
        world.setBlockState(getPos(), getBlockState().with(FILTER, status));
    }
}