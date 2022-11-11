package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityPowerFlower;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class ItemMatterUpgrader extends Item {
    public ItemMatterUpgrader() {
        super(new Properties().tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Component.translatable("item.projectexpansion.matter_upgrader.tooltip").setStyle(ColorStyle.GRAY));
        list.add(Component.translatable("item.projectexpansion.matter_upgrader.tooltip2").setStyle(ColorStyle.GREEN));
        list.add(Component.translatable("item.projectexpansion.matter_upgrader.tooltip_creative").setStyle(ColorStyle.RED));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        @Nullable Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

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
            player.displayClientMessage(Component.translatable("item.projectexpansion.matter_upgrader.max_upgrade").setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }


        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            player.displayClientMessage(Component.translatable("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable UUID owner = null;
        @Nullable String ownerName = null;
        @Nullable BigInteger emc = null;

        if (tile instanceof BlockEntityCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if (tile instanceof BlockEntityPowerFlower blockEntityPowerFlower) {
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            owner = blockEntityPowerFlower.owner;
            ownerName = blockEntityPowerFlower.ownerName;
            emc = blockEntityPowerFlower.emc;
            if (owner == null) return InteractionResult.FAIL;
            if (owner != player.getUUID()) {
                player.displayClientMessage(Component.translatable("item.projectexpansion.matter_upgrader.not_owner").setStyle(ColorStyle.RED), true);
                return InteractionResult.FAIL;
            }
        }

        if (tile instanceof BlockEntityRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if (tile instanceof BlockEntityEMCLink) {
            upgrade = Objects.requireNonNull(upgradeTo.getEMCLinkItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getEMCLink());
        }

        if (!provider.hasKnowledge(new ItemStack(upgrade)) && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("item.projectexpansion.matter_upgrader.not_learned", Component.translatable(Objects.requireNonNull(upgrade).toString())).setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if (player.isCreative()) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if (newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(Component.translatable("item.projectexpansion.matter_upgrader.not_enough_emc", EMCFormat.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.RED), true);
            return InteractionResult.FAIL;
        }

        level.removeBlock(pos, false);
        BlockState state = upgradeBlock.defaultBlockState();
        level.setBlockAndUpdate(pos, state);

        if (tile instanceof BlockEntityPowerFlower) {
            if (ownerName == null || emc == null) return InteractionResult.FAIL;

            BlockEntityPowerFlower newTile = new BlockEntityPowerFlower(pos, state);
            newTile.owner = owner;
            newTile.ownerName = ownerName;
            newTile.emc = emc;
            newTile.saveAdditional(new CompoundTag());
            level.removeBlockEntity(pos);
            level.setBlockEntity(newTile);
            Util.markDirty(level, pos);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(Component.translatable("item.projectexpansion.matter_upgrader.done", EMCFormat.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.WHITE), true);
        return InteractionResult.SUCCESS;
    }
}
