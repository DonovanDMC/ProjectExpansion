package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockRelay extends Block {
    public final Matter matter;

    public BlockRelay(Matter matter) {
        super(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(5F));
        this.matter = matter;
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
    public void addInformation(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, level, list, flag);
        list.add(new TranslationTextComponent("block.projectexpansion.relay.tooltip").mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("block.projectexpansion.relay.bonus", new StringTextComponent("").appendSibling(TransmutationEMCFormatter.formatEMC(matter.getRelayBounsForTicks(Config.tickDelay.get()))).mergeStyle(TextFormatting.GREEN)).mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("block.projectexpansion.relay.transfer", new StringTextComponent("").appendSibling(TransmutationEMCFormatter.formatEMC(matter.getRelayTransferForTicks(Config.tickDelay.get()))).mergeStyle(TextFormatting.GREEN)).mergeStyle(TextFormatting.GRAY));
    }
}
