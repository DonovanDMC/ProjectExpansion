package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.gui.container.ContainerPowerFlower;
import cool.furry.mc.projectexpansion.util.*;
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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

public class BlockPowerFlower extends Block implements IHasMatter, IMatterBlock, IUsableBlock {
    private static final VoxelShape SHAPE = VoxelShapes.or(
            box(0, 0, 0, 16, 1, 16),
            box(3.5, 4, 6.5, 12.5, 13, 9.5),
            box(6.5, 1, 6.5, 9.5, 16, 9.5),
            box(6.5, 4, 3.5, 9.5, 13, 12.5),
            box(6.5, 7, 0.5, 9.5, 10, 15.5),
            box(3.5, 7, 3.5, 12.5, 10, 12.5),
            box(0.5, 7, 6.5, 15.5, 10, 9.5)
    );
    private final Matter matter;

    public BlockPowerFlower(AbstractBlock.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static AbstractBlock.Properties getProperties(Matter matter) {
        return Block.Properties.of(Material.STONE, state -> matter.mapColor == null ? Material.STONE.getColor() : matter.mapColor.get()).strength(getDestroyTime(matter), getExplosionResistance(matter)).lightLevel((state) -> Math.min(matter.ordinal(), 15));
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
        return new BlockEntityPowerFlower();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos, ISelectionContext ctx) {
        return SHAPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.POWER_FLOWER_TOOLTIP.translateColored(TextFormatting.GRAY, new StringTextComponent(Config.server.tickDelay.get().toString()).setStyle(ColorStyle.GREEN), new StringTextComponent(Config.server.tickDelay.get() == 1 ? "" : "s").setStyle(ColorStyle.GRAY)));
        list.add(Lang.Blocks.POWER_FLOWER_EMC.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput()).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.POWER_FLOWER_STACK_EMC.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getPowerFlowerOutput().multiply(BigInteger.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
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
        BlockEntityPowerFlower blockEntity = WorldHelper.getTileEntity(BlockEntityPowerFlower.class, level, pos);
        if (blockEntity == null) return ActionResultType.FAIL;
        // only the owner gets the settings, anyone else is told who that is
        if (blockEntity.owner.equals(player.getUUID()) && player instanceof ServerPlayerEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, new SimpleNamedContainerProvider((id, inventory, p) -> new ContainerPowerFlower(id, inventory, blockEntity), getName()), pos);
            return ActionResultType.CONSUME;
        }
        blockEntity.handleActivation(player, BlockEntityOwnable.ActivationType.DISPLAY_NAME);
        return ActionResultType.PASS;
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        BlockEntityPowerFlower blockEntity = WorldHelper.getTileEntity(BlockEntityPowerFlower.class, level, pos);
        if (blockEntity == null) return;
        blockEntity.handlePlace(livingEntity, stack);
    }

    @Nullable

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

}
