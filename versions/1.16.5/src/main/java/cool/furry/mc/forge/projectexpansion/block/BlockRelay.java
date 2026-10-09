package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockRelay extends Block implements IHasMatter, IMatterBlock {
    private final Matter matter;

    public BlockRelay(AbstractBlock.Properties properties, Matter matter) {
        super(properties);
        this.matter = matter;
    }

    public static AbstractBlock.Properties getProperties(Matter matter) {
        return Block.Properties.of(Material.STONE, state -> matter.mapColor == null ? Material.STONE.getColor() : matter.mapColor.get()).strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        switch (matter) {
            case BASIC: return 10F;
            case DARK: return 1_000_000;
            default: return 2_000_000;
        }
    }

    private static float getExplosionResistance(Matter matter) {
        switch (matter) {
            case BASIC: return 30F;
            case DARK: return 3_000_000;
            default: return 6_000_000;
        }
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public EnumMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }


    @org.jetbrains.annotations.Nullable
    // block entities are created through forge's hooks before 1.17
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader level) {
        return new BlockEntityRelay();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.RELAY_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Blocks.RELAY_BONUS.translateColored(TextFormatting.GRAY, EMCFormat.getComponent(getMatter().getRelayBonusForTicks(Config.server.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.Blocks.RELAY_TRANSFER.translateColored(TextFormatting.GRAY, getMatter().getRelayTransferComponent().setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(TextFormatting.AQUA));
    }

    @Nullable

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

}
