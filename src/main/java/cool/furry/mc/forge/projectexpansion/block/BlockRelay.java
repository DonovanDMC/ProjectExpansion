package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BlockRelay extends Block implements IHasMatter, EntityBlock, IMatterBlock {
    private final Matter matter;

    public BlockRelay(Matter matter) {
        super(Block.Properties.of().strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    private static float getDestroyTime(Matter matter) {
        return switch (matter) {
            case BASIC -> 10F;
            case DARK -> 1_000_000;
            default -> 2_000_000;
        };
    }

    private static float getExplosionResistance(Matter matter) {
        return switch (matter) {
            case BASIC -> 30F;
            case DARK -> 3_000_000;
            default -> 6_000_000;
        };
    }

    @Override
    public @NotNull Matter getMatter() {
        return matter;
    }

    @Override
    public IMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }


    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRelay(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.RELAY_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.RELAY_BONUS.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getRelayBonusForTicks(Config.server.tickDelay.get())).setStyle(ColorStyle.GREEN)));
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
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return matter.mapColor == null ? super.getMapColor(state, level, pos, defaultColor) : matter.mapColor.get();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockTypes.RELAY.get();
    }
}
