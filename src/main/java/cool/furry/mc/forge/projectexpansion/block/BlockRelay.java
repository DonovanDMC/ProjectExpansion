package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockRelay extends Block implements IHasMatter {
    private final Matter matter;

    public BlockRelay(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(10, 30).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileRelay();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.RELAY_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.RELAY_BONUS.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getRelayBonusForTicks(Config.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.RELAY_TRANSFER.translateColored(TextFormatting.GRAY, getMatter().getRelayTransferComponent().setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
