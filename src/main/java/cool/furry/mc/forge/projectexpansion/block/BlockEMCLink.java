package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockEMCLink extends Block implements IHasMatter, EntityBlock {
    private final Matter matter;

    public BlockEMCLink(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(3.5F));
        this.matter = matter;
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockEntityNBTFilterable.FILTER, true));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityEMCLink(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add((new TranslatableComponent("block.projectexpansion.emc_link.tooltip")).setStyle(ColorStyle.GRAY));
        list.add((new TranslatableComponent("block.projectexpansion.emc_link.limit_items", matter.getEMCLinkItemLimitComponent().setStyle(ColorStyle.GREEN))).setStyle(ColorStyle.GRAY));
        list.add((new TranslatableComponent("block.projectexpansion.emc_link.limit_fluids", matter.getEMCLinkFluidLimitComponent().setStyle(ColorStyle.GREEN))).setStyle(ColorStyle.GRAY));
        list.add((new TranslatableComponent("block.projectexpansion.emc_link.limit_emc", new TextComponent(matter.getLevel() == 16 ? "INFINITY" : EMCFormat.format(matter.getEMCLinkEMCLimit())).setStyle(ColorStyle.GREEN))).setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BlockEntityEMCLink be) return be.handleActivation(player, hand);
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity livingEntity, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BlockEntityEMCLink be) be.wasPlaced(livingEntity, stack);
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockEntityNBTFilterable.FILTER);
        super.createBlockStateDefinition(builder);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.EMC_LINK.get() && !level.isClientSide) return BlockEntityEMCLink::tickServer;
        return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
