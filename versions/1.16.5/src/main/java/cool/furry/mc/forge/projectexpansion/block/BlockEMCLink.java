package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockEMCLink extends Block implements IHasMatter, IMatterBlock, IUsableBlock {
    private final Matter matter;

    public BlockEMCLink(AbstractBlock.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockEntityNBTFilterable.FILTER, true));
    }

    public static AbstractBlock.Properties getProperties(Matter matter) {
        return AbstractBlock.Properties.of(Material.STONE, state -> matter.mapColor == null ? Material.STONE.getColor() : matter.mapColor.get()).strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        switch (matter) {
            case BASIC: return 1.5F;
            case DARK: return 1_000_000;
            default: return 2_000_000;
        }
    }

    private static float getExplosionResistance(Matter matter) {
        switch (matter) {
            case BASIC: return 30F;
            case DARK: return 3_000_000;
            default: return 6_000_000;
        }
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
    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityEMCLink();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_LINK_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_ITEMS.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkItemLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_FLUIDS.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkFluidLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_FLUID_EXPORT_EFFICIENCY.translateColored(TextFormatting.GRAY, new StringTextComponent(getMatter().getFluidEfficiencyPercentage() + "%").setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_EMC.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkEMCLimitComponent()));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        return IUsableBlock.super.use(state, level, pos, player, hand, hit);
    }


    @Override
    public ActionResultType useWithoutItem(BlockState state, World level, BlockPos pos, PlayerEntity player, BlockRayTraceResult hitResult) {
        if (level.isClientSide) return ActionResultType.SUCCESS;
        BlockEntityEMCLink blockEntity = WorldHelper.getTileEntity(BlockEntityEMCLink.class, level, pos);
        if (blockEntity == null) return ActionResultType.FAIL;
        Hand hand = player.getUsedItemHand();
        return blockEntity.handleActivation(player, hand);
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        BlockEntityEMCLink blockEntity = WorldHelper.getTileEntity(BlockEntityEMCLink.class, level, pos);
        if (blockEntity == null) return;
        blockEntity.handlePlace(livingEntity, stack);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BlockEntityNBTFilterable.FILTER);
        super.createBlockStateDefinition(builder);
    }

    @Nullable

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

}
