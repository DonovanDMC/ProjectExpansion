package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemUpgrade extends Item {
    public ItemUpgrade() {
        super(new Item.Properties().tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(new TranslatableComponent("item.projectexpansion.matter_upgrader.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslatableComponent("item.projectexpansion.matter_upgrader.tooltip2").setStyle(ColorStyle.GREEN));
        list.add(new TranslatableComponent("item.projectexpansion.matter_upgrader.tooltip_creative").setStyle(ColorStyle.RED));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        @Nullable Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        ItemStack itemStack = context.getItemInHand();

        if (level.isClientSide || player == null) return InteractionResult.PASS;

        BlockEntity tile = level.getBlockEntity(pos);
        Block block = level.getBlockState(pos).getBlock();

        Matter matter;
        Matter upgradeTo;
        if (block instanceof IHasMatter) {
            matter = ((IHasMatter) block).getMatter();
            upgradeTo = matter.next();
        } else return InteractionResult.PASS;

        if (matter == Matter.FINAL) {
            player.displayClientMessage(new TranslatableComponent("item.projectexpansion.matter_upgrader.max_upgrade").setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable UUID owner = null;
        @Nullable String ownerName = null;
        @Nullable BigInteger emc = null;

        if(tile instanceof BlockEntityCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if(tile instanceof BlockEntityPowerFlower blockEntityPowerFlower) {
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            owner = blockEntityPowerFlower.owner;
            ownerName = blockEntityPowerFlower.ownerName;
            emc = blockEntityPowerFlower.emc;
            if(owner == null) return InteractionResult.FAIL;
            if(owner != player.getUUID()) {
                player.displayClientMessage(new TranslatableComponent("item.projectexpansion.matter_upgrader.not_owner").setStyle(ColorStyle.RED), true);
                return InteractionResult.FAIL;
            }
        }

        if(tile instanceof BlockEntityRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if(!provider.hasKnowledge(new ItemStack(upgrade)) && !player.isCreative()) {
            assert upgrade != null;
            player.displayClientMessage(new TranslatableComponent("item.projectexpansion.matter_upgrader.not_learned", new TranslatableComponent(upgrade.toString())).setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if(player.isCreative()) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if(newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(new TranslatableComponent("item.projectexpansion.matter_upgrader.not_enough_emc", EMCFormat.INSTANCE.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }

        assert upgradeBlock != null;
        level.removeBlock(pos, false);
        BlockState state = upgradeBlock.defaultBlockState();
        level.setBlockAndUpdate(pos, state);

        if(tile instanceof BlockEntityPowerFlower) {
            if(ownerName == null || emc == null) return InteractionResult.FAIL;

            BlockEntityPowerFlower newTile = new BlockEntityPowerFlower(pos, state);
            newTile.owner = owner;
            newTile.ownerName = ownerName;
            newTile.emc = emc;
            level.removeBlockEntity(pos);
            level.setBlockEntity(newTile);
            Util.markDirty(level, pos);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(new TranslatableComponent("item.projectexpansion.matter_upgrader.done", EMCFormat.INSTANCE.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.WHITE), true);
        return InteractionResult.SUCCESS;
    }
}
