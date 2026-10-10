package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import moze_intel.projecte.gameObjs.blocks.BlockDirection;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class BlockCollector extends BlockDirection implements IHasMatter, EntityBlock, IMatterBlock, IUsableBlock {
    private final Matter matter;

    public BlockCollector(BlockBehaviour.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static BlockBehaviour.Properties getProperties(Matter matter) {
        return Platform.blockProperties(matter).strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        return switch (matter) {
            case BASIC -> 0.3F;
            case DARK -> 1_000_000;
            default -> 2_000_000;
        };
    }

    private static float getExplosionResistance(Matter matter) {
        return switch (matter) {
            case BASIC -> 0.9F;
            case DARK -> 3_000_000;
            default -> 6_000_000;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCollector(pos, state);
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.COLLECTOR_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.COLLECTOR_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.COLLECTOR_STACK_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get()).multiply(BigDecimal.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.Blocks.COLLECTOR_MAX_STORAGE.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(Fuel.getCollectorEMCLimit(getMatter())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.COLLECTOR.get() && !level.isClientSide) return BlockEntityCollector::tickServer;
        return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return IUsableBlock.super.use(state, level, pos, player, hand, hit);
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
        if (collector == null) return InteractionResult.FAIL;
        Platform.openScreen((ServerPlayer) player, collector, pos);

        return InteractionResult.CONSUME;
    }

    @Override
    @Deprecated
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        CollectorMK1BlockEntity collector = WorldHelper.getBlockEntity(CollectorMK1BlockEntity.class, level, pos, true);
        if (collector == null) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        Optional<IItemHandler> cap = collector.getCapability(Platform.ITEM_HANDLER, Direction.UP).resolve();
        if (cap.isEmpty()) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        ItemStack charging = cap.get().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT);
        if (charging.isEmpty()) {
            return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getMaximumEmc());
        }
        Optional<IItemEmcHolder> holderCapability = charging.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
        if (holderCapability.isPresent()) {
            IItemEmcHolder emcHolder = holderCapability.get();
            return Util.scaleToRedstone(emcHolder.getStoredEmc(charging), emcHolder.getMaximumEmc(charging));
        }
        return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getEmcToNextGoal());
    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
            if (collector != null) {
                //Clear the ghost slot so calling super doesn't drop the item in it
                collector.clearLocked();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
