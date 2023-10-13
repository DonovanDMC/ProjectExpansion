package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TileEMCLink;
import cool.furry.mc.forge.projectexpansion.tile.TileNBTFilterable;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Matter;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockEMCLink extends Block implements IHasMatter {
    private final Matter matter;

    public BlockEMCLink(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(1.5F, 30).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
        this.registerDefaultState(this.stateDefinition.any().setValue(TileNBTFilterable.FILTER, true));
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileEMCLink();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_LINK_TOOLTIP.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkItemLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_ITEMS.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkFluidLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_FLUIDS.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkEMCLimitComponent()));
        list.add(Lang.Blocks.EMC_LINK_FLUID_EXPORT_EFFICIENCY.translateColored(TextFormatting.GRAY, new StringTextComponent(getMatter().getFluidEfficiencyPercentage() + "%").setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.EMC_LINK_LIMIT_EMC.translateColored(TextFormatting.GRAY, getMatter().getEMCLinkEMCLimitComponent()));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (level.isClientSide) return ActionResultType.SUCCESS;
        TileEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEMCLink) return ((TileEMCLink) blockEntity).handleActivation(player, hand);
        return ActionResultType.PASS;
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEMCLink) ((TileEMCLink) blockEntity).handlePlace(livingEntity, stack);
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(TileNBTFilterable.FILTER);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
