package cool.furry.mc.projectexpansion.block.entity;

import cool.furry.mc.projectexpansion.util.ColorStyle;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.TagNames;
import cool.furry.mc.projectexpansion.util.Util;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlockEntityOwnable extends BlockEntityBase {
    public UUID owner = new UUID(0L, 0L);
    public String ownerName = "";

    public BlockEntityOwnable(TileEntityType<?> type) {
        super(type);
    }

    @Override
    protected void loadData(CompoundNBT tag) {
        super.loadData(tag);
        if (tag.hasUUID(TagNames.OWNER)) owner = tag.getUUID(TagNames.OWNER);
        if (tag.contains(TagNames.OWNER_NAME, Constants.NBT.TAG_STRING)) ownerName = tag.getString(TagNames.OWNER_NAME);
    }

    @Override
    protected void saveData(CompoundNBT tag) {
        super.saveData(tag);
        tag.putUUID(TagNames.OWNER, owner);
        tag.putString(TagNames.OWNER_NAME, ownerName);
    }

    public void setOwner(PlayerEntity player) {
        owner = player.getUUID();
        ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }

    public enum ActivationType {
        DISPLAY_NAME,
        CHECK_OWNERSHIP
    }

    // return true if ownership not checked, or if passed
    public boolean handleActivation(PlayerEntity player, ActivationType activationType) {
        switch (activationType) {
            case DISPLAY_NAME: player.displayClientMessage(new StringTextComponent(ownerName), true); break;
            case CHECK_OWNERSHIP: {
                    if (!owner.equals(player.getUUID())) {
                        player.displayClientMessage(Lang.NOT_OWNER.translateColored(TextFormatting.RED, new StringTextComponent(ownerName).setStyle(ColorStyle.RED)), true);
                        return false;
                    }
                } break;
        }

        return true;
    }

    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner(((PlayerEntity) livingEntity));
    }
}
