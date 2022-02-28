package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TilePersonalLink;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

import javax.annotation.Nullable;
import java.util.List;

public class BlockPersonalLink extends Block {
    public BlockPersonalLink() {
        super(AbstractBlock.Properties.create(Material.ROCK).setRequiresTool().hardnessAndResistance(5F).sound(SoundType.STONE));
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TilePersonalLink();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add((new TranslationTextComponent("block.projectexpansion.personal_link.tooltip")).mergeStyle(TextFormatting.GRAY));
    }

    @Deprecated
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TilePersonalLink) player.sendStatusMessage(new StringTextComponent(((TilePersonalLink) tile).ownerName), true);
        return super.onBlockActivated(state, world, pos, player, hand, ray);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if(tile instanceof TilePersonalLink) ((TilePersonalLink) tile).wasPlaced(livingEntity, stack);
    }
}
