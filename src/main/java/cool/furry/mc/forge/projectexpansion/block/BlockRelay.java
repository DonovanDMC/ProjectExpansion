package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.init.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockRelay extends Block implements IHasMatter, EntityBlock {
    private final Matter matter;

    public BlockRelay(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(5F));
        this.matter = matter;
    }

    @Nonnull
    @Override
    public Matter getMatter() {
        return matter;
    }


    @org.jetbrains.annotations.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRelay(pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("block.projectexpansion.relay.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("text.projectexpansion.see_wiki").setStyle(ColorStyle.AQUA));
        list.add(new TranslatableComponent("block.projectexpansion.relay.bonus", TransmutationEMCFormatter.formatEMC(matter.getRelayBonusForTicks(Config.tickDelay.get())).copy().setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("block.projectexpansion.relay.transfer", TransmutationEMCFormatter.formatEMC(matter.getRelayTransferForTicks(Config.tickDelay.get())).copy().setStyle(ColorStyle.GREEN)).setStyle(ColorStyle.GRAY));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.RELAY.get() && !level.isClientSide)
            return BlockEntityRelay::tickServer;
        return null;
    }
}
