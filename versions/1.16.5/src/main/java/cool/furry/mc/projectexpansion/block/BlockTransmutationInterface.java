package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityTransmutationInterface;
import cool.furry.mc.projectexpansion.util.IUsableBlock;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Matter;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockTransmutationInterface extends Block implements IMatterBlock, IUsableBlock {
    public BlockTransmutationInterface(AbstractBlock.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockEntityNBTFilterable.FILTER, true));
    }

    public static AbstractBlock.Properties getProperties() {
        return Block.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(1_000_000, 2_000_000).requiresCorrectToolForDrops().lightLevel((state) -> 15);
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(Matter.FINAL);
    }

    @Nullable
    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityTransmutationInterface();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.TRANSMUTATION_INTERFACE_TOOLTIP.translateColored(TextFormatting.GRAY));
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

        BlockEntityTransmutationInterface blockEntity = WorldHelper.getTileEntity(BlockEntityTransmutationInterface.class, level, pos);
        if (blockEntity == null) return ActionResultType.FAIL;

        blockEntity.handleActivation(player, BlockEntityOwnable.ActivationType.DISPLAY_NAME);
        return ActionResultType.PASS;
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        BlockEntityTransmutationInterface blockEntity = WorldHelper.getTileEntity(BlockEntityTransmutationInterface.class, level, pos);
        if (blockEntity == null) return;
        blockEntity.handlePlace(entity, stack);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BlockEntityNBTFilterable.FILTER);
    }

    @Nullable

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

}
