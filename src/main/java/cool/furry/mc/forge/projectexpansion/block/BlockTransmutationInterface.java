package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import cool.furry.mc.forge.projectexpansion.tile.TileTransmutationInterface;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTransmutationInterface extends Block {
    public BlockTransmutationInterface() {
        super(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(5F));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileTransmutationInterface();
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TileTransmutationInterface) player.sendStatusMessage(new StringTextComponent(((TileTransmutationInterface) tile).ownerName), true);
        return super.onBlockActivated(state, world, pos, player, hand, ray);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TileTransmutationInterface) ((TileTransmutationInterface) tile).wasPlaced(livingEntity, stack);
    }
}
