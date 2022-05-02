package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TileTransmutationInterface;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockTransmutationInterface extends Block {
    public BlockTransmutationInterface() {
        super(Block.Properties.create(Material.ROCK).hardnessAndResistance(5F));
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

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add(new TranslationTextComponent("block.projectexpansion.transmutation_interface.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isRemote)
            return true;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileTransmutationInterface)
            player.sendStatusMessage(new StringTextComponent(((TileTransmutationInterface) tile).ownerName), true);
        return super.onBlockActivated(state, world, pos, player, hand, ray);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileTransmutationInterface)
            ((TileTransmutationInterface) tile).wasPlaced(livingEntity, stack);
    }

    @Override
    public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type) {
        return false;
    }
}
