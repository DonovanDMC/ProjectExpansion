package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.entity.*;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
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

public class ItemMatterUpgrader extends Item {
    public ItemMatterUpgrader() {
        super(new Properties().tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP2.translateColored(ChatFormatting.GREEN));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP_CREATIVE.translateColored(ChatFormatting.RED));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        @Nullable Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        if (level.isClientSide || player == null) return InteractionResult.PASS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = level.getBlockState(pos).getBlock();

        Matter matter;
        Matter upgradeTo;
        if (block instanceof IHasMatter) {
            matter = ((IHasMatter) block).getMatter();
            upgradeTo = matter.next();
        } else return InteractionResult.PASS;

        if (matter == Matter.FINAL) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_MAX_UPGRADE.translateColored(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }


        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, player.getDisplayName()), true);
            return InteractionResult.FAIL;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable BlockEntity newBlockEntity = null;
        @Nullable BlockState newBlockState = null;

        if (blockEntity instanceof BlockEntityCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if (blockEntity instanceof BlockEntityPowerFlower be) {
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            if (be.owner == null) return InteractionResult.FAIL;
            if (!be.owner.equals(player.getUUID())) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockEntityPowerFlower intBlockEntity = new BlockEntityPowerFlower(pos, newBlockState = upgradeBlock.defaultBlockState());
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            intBlockEntity.saveAdditional(new CompoundTag());
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof BlockEntityEMCLink be) {
            upgrade = Objects.requireNonNull(upgradeTo.getEMCLinkItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getEMCLink());
            if (be.owner == null) return InteractionResult.FAIL;
            if (be.owner != player.getUUID()) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockEntityEMCLink intBlockEntity = new BlockEntityEMCLink(pos, newBlockState = upgradeBlock.defaultBlockState().setValue(BlockEntityNBTFilterable.FILTER, be.getBlockState().getValue(BlockEntityNBTFilterable.FILTER)));
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            intBlockEntity.itemStack = be.itemStack;
            intBlockEntity.remainingEMC = be.remainingEMC;
            intBlockEntity.remainingImport = be.remainingImport;
            intBlockEntity.remainingExport = be.remainingExport;
            intBlockEntity.remainingFluid = be.remainingFluid;
            intBlockEntity.saveAdditional(new CompoundTag());
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof BlockEntityRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if (!provider.hasKnowledge(new ItemStack(upgrade)) && !player.isCreative()) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_LEARNED.translateColored(ChatFormatting.RED, Component.translatable(Objects.requireNonNull(upgrade).toString())), true);
            return InteractionResult.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if (player.isCreative()) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if (newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_ENOUGH_EMC.translateColored(ChatFormatting.RED, EMCFormat.format(BigInteger.valueOf(diff))), true);
            return InteractionResult.FAIL;
        }

        if(newBlockState == null) {
            newBlockState = upgradeBlock.defaultBlockState();
        }

        level.removeBlock(pos, false);
        level.setBlockAndUpdate(pos, newBlockState);

        if(newBlockEntity != null) {
            level.removeBlockEntity(pos);
            level.setBlockEntity(newBlockEntity);
            Util.markDirty(level, pos);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(Lang.Items.MATTER_UPGRADER_DONE.translateColored(ChatFormatting.WHITE, EMCFormat.format(BigInteger.valueOf(diff))), true);
        return InteractionResult.SUCCESS;
    }
}
