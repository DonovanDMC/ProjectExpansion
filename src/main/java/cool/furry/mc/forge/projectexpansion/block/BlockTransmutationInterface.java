package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.tile.TileNBTFilterable;
import cool.furry.mc.forge.projectexpansion.tile.TileTransmutationInterface;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
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

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockTransmutationInterface extends Block {
    public BlockTransmutationInterface() {
        super(Block.Properties.of(Material.STONE).strength(1.5F, 30).requiresCorrectToolForDrops().lightLevel((state) -> 15));
        this.registerDefaultState(this.stateDefinition.any().setValue(TileNBTFilterable.FILTER, true));
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
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.TRANSMUTATION_INTERFACE_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        if (world.isClientSide) return ActionResultType.SUCCESS;
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileTransmutationInterface) player.displayClientMessage(new StringTextComponent(((TileTransmutationInterface) tile).ownerName), true);
        return super.use(state, world, pos, player, hand, ray);
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileTransmutationInterface) ((TileTransmutationInterface) tile).handlePlace(livingEntity, stack);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TileNBTFilterable.FILTER);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
