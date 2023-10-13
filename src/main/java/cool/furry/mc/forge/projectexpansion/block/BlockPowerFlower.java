package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
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
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockPowerFlower extends Block implements IHasMatter {
    public static final VoxelShape SHAPE = VoxelShapes.or(
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
        super(Block.Properties.of(Material.STONE).strength(1.5F, 30).lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TilePowerFlower();
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext ctx) {
        return SHAPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.POWER_FLOWER_TOOLTIP.translateColored(TextFormatting.GRAY, new StringTextComponent(Config.tickDelay.get().toString()).setStyle(ColorStyle.GREEN), new StringTextComponent(Config.tickDelay.get() == 1 ? "" : "s").setStyle(ColorStyle.GRAY)));
        list.add(Lang.Blocks.POWER_FLOWER_EMC.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput()).setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Deprecated
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isClientSide) return ActionResultType.SUCCESS;
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TilePowerFlower) player.displayClientMessage(new StringTextComponent(((TilePowerFlower) tile).ownerName), true);
        return super.use(state, world, pos, player, hand, ray);
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TilePowerFlower) ((TilePowerFlower) tile).handlePlace(livingEntity, stack);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
