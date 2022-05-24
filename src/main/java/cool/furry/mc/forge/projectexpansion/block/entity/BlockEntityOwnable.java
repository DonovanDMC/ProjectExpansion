package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class BlockEntityOwnable extends BlockEntity {
    public UUID owner = new UUID(0L, 0L);
    public String ownerName = "";

    public BlockEntityOwnable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if(tag.hasUUID(TagNames.OWNER)) owner = tag.getUUID(TagNames.OWNER);
        if(tag.contains(TagNames.OWNER_NAME, Tag.TAG_STRING)) ownerName = tag.getString(TagNames.OWNER_NAME);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID(TagNames.OWNER, owner);
        tag.putString(TagNames.OWNER_NAME, ownerName);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getScoreboardName();
        Util.markDirty(this);
    }
}
