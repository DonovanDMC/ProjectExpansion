package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityTransmutationInterface;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockTransmutationInterface extends Block implements EntityBlock {
    public BlockTransmutationInterface() {
        super(Block.Properties.of(Material.STONE).strength(5F));
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTransmutationInterface(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("block.projectexpansion.transmutation_interface.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity block = level.getBlockEntity(pos);
        if (block instanceof BlockEntityTransmutationInterface) player.displayClientMessage(new TextComponent(((BlockEntityTransmutationInterface) block).ownerName), true);
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity instanceof BlockEntityTransmutationInterface be) be.wasPlaced(entity, stack);
    }

    // @TODO make immovable

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if(type == BlockEntityTypes.TRANSMUTATION_INTERFACE.get() && !level.isClientSide) return BlockEntityTransmutationInterface::tickServer;
        return null;
    }
}
