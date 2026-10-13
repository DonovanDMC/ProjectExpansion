package cool.furry.mc.projectexpansion.block;

import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCRefinery;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Common;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockEMCRefinery extends HorizontalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BlockEMCRefinery(AbstractBlock.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public static AbstractBlock.Properties getProperties() {
        return AbstractBlock.Properties.of(Material.STONE).strength(10, 30).requiresCorrectToolForDrops().harvestTool(ToolType.PICKAXE).lightLevel(state -> state.getValue(LIT) ? 13 : 0);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockItemUseContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        TileEntity refinery = level.getBlockEntity(pos);
        if (!level.isClientSide && player instanceof ServerPlayerEntity && refinery instanceof BlockEntityEMCRefinery) {
            Common.openContainer((ServerPlayerEntity) player, (BlockEntityEMCRefinery) refinery, pos);
        }
        return ActionResultType.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, World level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TileEntity refinery = level.getBlockEntity(pos);
        if (!state.is(newState.getBlock()) && refinery instanceof BlockEntityEMCRefinery) {
            IItemHandler items = ((BlockEntityEMCRefinery) refinery).getItems();
            for (int i = 0; i < items.getSlots(); i++) Block.popResource(level, pos, items.getStackInSlot(i));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // needs a dark matter pickaxe like the ore does
    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader level, BlockPos pos, PlayerEntity player) {
        return BlockIanthiumOre.isMatterPickaxe(player.getMainHandItem());
    }

    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityEMCRefinery();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.EMC_REFINERY_TOOLTIP.translateColored(TextFormatting.GRAY));
    }
}
