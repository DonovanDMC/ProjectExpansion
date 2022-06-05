package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;

import java.util.UUID;

public class TileOwnable extends TileEntity {
    public UUID owner = new UUID(0L, 0L);
    public String ownerName = "";
    public TileOwnable(TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.hasUniqueId(NBTNames.OWNER)) owner = nbt.getUniqueId(NBTNames.OWNER);
        if (nbt.contains(NBTNames.OWNER_NAME, Constants.NBT.TAG_STRING)) ownerName = nbt.getString(NBTNames.OWNER_NAME);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        nbt.putUniqueId(NBTNames.OWNER, owner);
        nbt.putString(NBTNames.OWNER_NAME, ownerName);
        return nbt;
    }

    public void setOwner(PlayerEntity player) {
        owner = player.getUniqueID();
        ownerName = player.getScoreboardName();
        markDirty();
    }

    public enum ActivationType {
        DISPLAY_NAME,
        CHECK_OWNERSHIP
    }
    // return true if ownership not checked, or if passed
    public boolean handleActivation(PlayerEntity player, ActivationType activationType) {
        switch (activationType) {
            case DISPLAY_NAME: {
                player.sendStatusMessage(new StringTextComponent(ownerName), true);
                break;
            }

            case CHECK_OWNERSHIP: {
                if (!owner.equals(player.getUniqueID())) {
                    player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.not_owner", new StringTextComponent(ownerName).setStyle(ColorStyle.RED)).setStyle(ColorStyle.RED), true);
                    return false;
                }
                break;
            }
        }

        return true;
    }
}
