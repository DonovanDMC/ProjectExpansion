package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

public class BlockPowerFlower extends Block implements IHasMatter, EntityBlock, IMatterBlock, IUsableBlock {
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

    public BlockPowerFlower(BlockBehaviour.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static BlockBehaviour.Properties getProperties(Matter matter) {
        return Platform.blockProperties(matter).strength(getDestroyTime(matter), getExplosionResistance(matter)).lightLevel((state) -> Math.min(matter.ordinal(), 15));
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
    public Matter getMatter() {
        return matter;
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPowerFlower(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.POWER_FLOWER_TOOLTIP.translateColored(ChatFormatting.GRAY, Platform.literal(Config.server.tickDelay.get().toString()).setStyle(ColorStyle.GREEN), Platform.literal(Config.server.tickDelay.get() == 1 ? "" : "s").setStyle(ColorStyle.GRAY)));
        list.add(Lang.Blocks.POWER_FLOWER_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput()).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.POWER_FLOWER_STACK_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput().multiply(BigInteger.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return IUsableBlock.super.use(state, level, pos, player, hand, hit);
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntityPowerFlower blockEntity = WorldHelper.getBlockEntity(BlockEntityPowerFlower.class, level, pos);
        if (blockEntity == null) return InteractionResult.FAIL;
        blockEntity.handleActivation(player, BlockEntityOwnable.ActivationType.DISPLAY_NAME);
        return InteractionResult.PASS;
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
}
