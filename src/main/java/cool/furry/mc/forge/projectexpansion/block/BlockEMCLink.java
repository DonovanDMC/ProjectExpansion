package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BlockEMCLink extends Block implements IHasMatter, EntityBlock, IMatterBlock {
    private final Matter matter;

    public BlockEMCLink(BlockBehaviour.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockEntityNBTFilterable.FILTER, true));
    }

    public static BlockBehaviour.Properties getProperties(Matter matter) {
        return BlockBehaviour.Properties.of().strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        return switch (matter) {
            case BASIC -> 1.5F;
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityEMCLink(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.EMC_LINK_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_ITEMS.translateColored(ChatFormatting.GRAY, getMatter().getEMCLinkItemLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_FLUIDS.translateColored(ChatFormatting.GRAY, getMatter().getEMCLinkFluidLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_FLUID_EXPORT_EFFICIENCY.translateColored(ChatFormatting.GRAY, Component.literal(getMatter().getFluidEfficiencyPercentage() + "%").setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_EMC.translateColored(ChatFormatting.GRAY, getMatter().getEMCLinkEMCLimitComponent()));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntityEMCLink blockEntity = WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos);
        if (blockEntity == null) return InteractionResult.FAIL;
        InteractionHand hand = player.getUsedItemHand();
        return blockEntity.handleActivation(player, hand);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        BlockEntityEMCLink blockEntity = WorldHelper.getBlockEntity(BlockEntityEMCLink.class, level, pos);
        if (blockEntity == null) return;
        blockEntity.handlePlace(livingEntity, stack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockEntityNBTFilterable.FILTER);
        super.createBlockStateDefinition(builder);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.EMC_LINK.get() && !level.isClientSide) return BlockEntityEMCLink::tickServer;
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
        return BlockTypes.EMC_LINK.get();
    }
}
