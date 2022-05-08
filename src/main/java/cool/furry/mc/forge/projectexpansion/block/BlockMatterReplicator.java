package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.init.Items;
import cool.furry.mc.forge.projectexpansion.tile.TileMatterReplicator;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockMatterReplicator extends ContainerBlock {
    public BlockMatterReplicator() {
        super(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(3.5F));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new TileMatterReplicator();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add(new TranslationTextComponent("block.projectexpansion.matter_replicator.tooltip").mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("block.projectexpansion.matter_replicator.tooltip2").mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").mergeStyle(TextFormatting.AQUA));
    }

    @Override
    public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type) {
        return false;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if(world.isRemote) return ActionResultType.SUCCESS;
        INamedContainerProvider container = this.getContainer(state, world, pos);
        if(container != null) NetworkHooks.openGui((ServerPlayerEntity) player, container, (buf) -> buf.writeBlockPos(pos));
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if(state.getBlock() != newState.getBlock()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if(tileEntity instanceof TileMatterReplicator) {
                TileMatterReplicator te = (TileMatterReplicator) tileEntity;
                NonNullList<ItemStack> items = NonNullList.create();
                if(te.speedUpgradeCount > 0) items.add(new ItemStack(Items.SPEED_UPGRADE.get(), te.speedUpgradeCount));
                if(te.stackUpgradeCount > 0) items.add(new ItemStack(Items.STACK_UPGRADE.get(), te.stackUpgradeCount));
                if(items.size() > 0) InventoryHelper.dropItems(world, pos, items);
                world.updateComparatorOutputLevel(pos, this);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity instanceof  TileMatterReplicator) return ((TileMatterReplicator) tileEntity).isLocked ? 0 : 1;
        return 0;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
