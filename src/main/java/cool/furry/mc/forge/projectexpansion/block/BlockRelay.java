package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockRelay extends Block implements IHasMatter, EntityBlock {
    private final Matter matter;

    public BlockRelay(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(10, 30).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }


    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRelay(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.RELAY_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.RELAY_BONUS.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getRelayBonusForTicks(Config.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.RELAY_TRANSFER.translateColored(ChatFormatting.GRAY, getMatter().getRelayTransferComponent().setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.RELAY.get() && !level.isClientSide) return BlockEntityRelay::tickServer;
        return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public MaterialColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MaterialColor defaultColor) {
        MaterialColor color = matter.materialColor == null ? null : matter.materialColor.get();
        return color != null ? color : super.getMapColor(state, level, pos, defaultColor);
    }
}
