package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.BlockDirection;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.gameObjs.tiles.CollectorMK1Tile;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class BlockCollector extends BlockDirection implements IHasMatter, IMatterBlock, IUsableBlock {
    private final Matter matter;

    public BlockCollector(AbstractBlock.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static AbstractBlock.Properties getProperties(Matter matter) {
        return Block.Properties.of(Material.STONE, state -> matter.mapColor == null ? Material.STONE.getColor() : matter.mapColor.get()).strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        switch (matter) {
            case BASIC: return 0.3F;
            case DARK: return 1_000_000;
            default: return 2_000_000;
        }
    }

    private static float getExplosionResistance(Matter matter) {
        switch (matter) {
            case BASIC: return 0.9F;
            case DARK: return 3_000_000;
            default: return 6_000_000;
        }
    }

    @Nullable
    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityCollector(matter);
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
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.COLLECTOR_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.COLLECTOR_EMC.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.COLLECTOR_STACK_EMC.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get()).multiply(BigDecimal.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.Blocks.COLLECTOR_MAX_STORAGE.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(Fuel.getCollectorEMCLimit(getMatter())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Nullable

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        return IUsableBlock.super.use(state, level, pos, player, hand, hit);
    }


    @Override
    public ActionResultType useWithoutItem(BlockState state, World level, BlockPos pos, PlayerEntity player, BlockRayTraceResult hitResult) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }

        BlockEntityCollector collector = WorldHelper.getTileEntity(BlockEntityCollector.class, level, pos);
        if (collector == null) return ActionResultType.FAIL;
        Common.openContainer((ServerPlayerEntity) player, collector, pos);

        return ActionResultType.CONSUME;
    }

    @Override
    @Deprecated
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
        CollectorMK1Tile collector = WorldHelper.getTileEntity(CollectorMK1Tile.class, level, pos, true);
        if (collector == null) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        Optional<IItemHandler> cap = collector.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).resolve();
        if (!cap.isPresent()) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        ItemStack charging = cap.get().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT);
        if (charging.isEmpty()) {
            return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getMaximumEmc());
        }
        Optional<IItemEmcHolder> holderCapability = charging.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
        if (holderCapability.isPresent()) {
            IItemEmcHolder emcHolder = holderCapability.get();
            return Util.scaleToRedstone(emcHolder.getStoredEmc(charging), emcHolder.getMaximumEmc(charging));
        }
        return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getEmcToNextGoal());
    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, World level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntityCollector collector = WorldHelper.getTileEntity(BlockEntityCollector.class, level, pos);
            if (collector != null) {
                //Clear the ghost slot so calling super doesn't drop the item in it
                collector.clearLocked();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

}
