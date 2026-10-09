package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.block.entity.*;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

public class ItemMatterUpgrader extends Item {
    @SuppressWarnings("unused")
    public ItemMatterUpgrader() {
        super(new Properties());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP2.translateColored(TextFormatting.GREEN));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP_CREATIVE.translateColored(TextFormatting.RED));
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        @Nullable PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        World level = context.getLevel();

        if (level.isClientSide || player == null) return ActionResultType.PASS;

        TileEntity blockEntity = level.getBlockEntity(pos);
        Block block = level.getBlockState(pos).getBlock();

        Matter matter;
        Matter upgradeTo;
        if (block instanceof IHasMatter) {
            matter = ((IHasMatter) block).getMatter();
            upgradeTo = matter.next();
        } else return ActionResultType.PASS;

        if (matter == Matter.FINAL) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_MAX_UPGRADE.translateColored(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }


        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            player.displayClientMessage(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, player.getDisplayName()), true);
            return ActionResultType.FAIL;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable TileEntity newBlockEntity = null;
        @Nullable BlockState newBlockState = null;

        if (blockEntity instanceof BlockEntityCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if (blockEntity instanceof BlockEntityPowerFlower) {
            BlockEntityPowerFlower be = ((BlockEntityPowerFlower) blockEntity);
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            if (be.owner == null) return ActionResultType.FAIL;
            if (!be.owner.equals(player.getUUID())) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }

            newBlockState = upgradeBlock.defaultBlockState();
            BlockEntityPowerFlower intBlockEntity = new BlockEntityPowerFlower();
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof BlockEntityEMCLink) {
            BlockEntityEMCLink be = ((BlockEntityEMCLink) blockEntity);
            upgrade = Objects.requireNonNull(upgradeTo.getEMCLinkItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getEMCLink());
            if (be.owner == null) return ActionResultType.FAIL;
            if (!be.owner.equals(player.getUUID())) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }

            newBlockState = upgradeBlock.defaultBlockState().setValue(BlockEntityNBTFilterable.FILTER, be.getBlockState().getValue(BlockEntityNBTFilterable.FILTER));
            BlockEntityEMCLink intBlockEntity = new BlockEntityEMCLink();
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            intBlockEntity.itemStack = be.itemStack;
            intBlockEntity.remainingEMC = be.remainingEMC;
            intBlockEntity.remainingImport = be.remainingImport;
            intBlockEntity.remainingExport = be.remainingExport;
            intBlockEntity.remainingFluid = be.remainingFluid;
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof BlockEntityRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if (upgrade == null || !provider.hasKnowledge(ItemInfo.fromItem(upgrade)) && !player.isCreative()) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_LEARNED.translateColored(TextFormatting.RED, new TranslationTextComponent(Objects.requireNonNull(upgrade).toString())), true);
            return ActionResultType.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if (player.isCreative()) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if (newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_ENOUGH_EMC.translateColored(TextFormatting.RED, EMCFormat.format(BigInteger.valueOf(diff))), true);
            return ActionResultType.FAIL;
        }

        if(newBlockState == null) {
            newBlockState = upgradeBlock.defaultBlockState();
        }

        level.removeBlock(pos, false);
        level.setBlockAndUpdate(pos, newBlockState);

        if(newBlockEntity != null) {
            level.removeBlockEntity(pos);
            level.setBlockEntity(pos, newBlockEntity);
            Util.markDirty(level, pos);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(Lang.Items.MATTER_UPGRADER_DONE.translateColored(TextFormatting.WHITE, EMCFormat.format(BigInteger.valueOf(diff))), true);
        return ActionResultType.SUCCESS;
    }
}
