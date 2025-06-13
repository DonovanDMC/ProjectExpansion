package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockPowerFlower extends Block implements IHasMatter, EntityBlock, IMatterBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 1, 16),
            box(3.5, 4, 6.5, 12.5, 13, 9.5),
            box(6.5, 1, 6.5, 9.5, 16, 9.5),
            box(6.5, 4, 3.5, 9.5, 13, 12.5),
            box(6.5, 7, 0.5, 9.5, 10, 15.5),
            box(3.5, 7, 3.5, 12.5, 10, 12.5),
            box(0.5, 7, 6.5, 15.5, 10, 9.5)
    );
    private final Matter matter;

    public BlockPowerFlower(Matter matter) {
        super(Block.Properties.of().strength(getDestroyTime(matter), getExplosionResistance(matter)).lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
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
        return new BlockEntityPowerFlower(pos, state);
    }

    @NotNull
    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.POWER_FLOWER_TOOLTIP.translateColored(ChatFormatting.GRAY, Component.literal(Config.server.tickDelay.get().toString()).setStyle(ColorStyle.GREEN), Component.literal(Config.server.tickDelay.get() == 1 ? "" : "s").setStyle(ColorStyle.GRAY)));
        list.add(Lang.Blocks.POWER_FLOWER_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput()).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.POWER_FLOWER_STACK_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput().multiply(BigInteger.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntityPowerFlower blockEntity = WorldHelper.getBlockEntity(BlockEntityPowerFlower.class, level, pos);
        if (blockEntity == null) return InteractionResult.FAIL;
        blockEntity.handleActivation(player, BlockEntityOwnable.ActivationType.DISPLAY_NAME);
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        BlockEntityPowerFlower blockEntity = WorldHelper.getBlockEntity(BlockEntityPowerFlower.class, level, pos);
        if (blockEntity == null) return;
        blockEntity.handlePlace(livingEntity, stack);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.POWER_FLOWER.get() && !level.isClientSide) return BlockEntityPowerFlower::tickServer;
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
        return BlockTypes.POWER_FLOWER.get();
    }
}
