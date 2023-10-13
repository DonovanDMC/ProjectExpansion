package cool.furry.mc.forge.projectexpansion.tile;

import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.NBTNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.UUID;

public class TileOwnable extends TileEntity {
    public UUID owner = new UUID(0L, 0L);
    public String ownerName = "";
    public TileOwnable(TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.hasUUID(NBTNames.OWNER)) owner = nbt.getUUID(NBTNames.OWNER);
        if (nbt.contains(NBTNames.OWNER_NAME, Constants.NBT.TAG_STRING)) ownerName = nbt.getString(NBTNames.OWNER_NAME);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putUUID(NBTNames.OWNER, owner);
        nbt.putString(NBTNames.OWNER_NAME, ownerName);
        return nbt;
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
            case DISPLAY_NAME: {
                player.displayClientMessage(new StringTextComponent(ownerName), true);
                break;
            }

            case CHECK_OWNERSHIP: {
                if (!owner.equals(player.getUUID())) {
                    player.displayClientMessage(Lang.NOT_OWNER.translateColored(TextFormatting.RED, new StringTextComponent(ownerName).setStyle(ColorStyle.RED)), true);
                    return false;
                }
                break;
            }
        }

        return true;
    }

    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof PlayerEntity) setOwner((PlayerEntity) livingEntity);
    }
}
