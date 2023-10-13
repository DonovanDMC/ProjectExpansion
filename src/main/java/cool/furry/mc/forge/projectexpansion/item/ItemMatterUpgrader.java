package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.*;
import cool.furry.mc.forge.projectexpansion.util.*;
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
import net.minecraft.nbt.CompoundNBT;
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
    public ItemMatterUpgrader() {
        super(new Item.Properties().tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP.translateColored(TextFormatting.GRAY));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP2.translateColored(TextFormatting.GREEN));
        list.add(Lang.Items.MATTER_UPGRADER_TOOLTIP_CREATIVE.translateColored(TextFormatting.RED));
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        @Nullable PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        World world = context.getLevel();

        if (world.isClientSide || player == null) return ActionResultType.PASS;

        TileEntity blockEntity = world.getBlockEntity(pos);
        Block block = world.getBlockState(pos).getBlock();

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

        if (blockEntity instanceof TileCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if (blockEntity instanceof TilePowerFlower) {
            TilePowerFlower be = (TilePowerFlower) blockEntity;
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            if (be.owner == null) return ActionResultType.FAIL;
            if (be.owner != player.getUUID()) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }

            TilePowerFlower intBlockEntity = new TilePowerFlower();
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            intBlockEntity.save(new CompoundNBT());
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof TileEMCLink) {
            TileEMCLink be = (TileEMCLink) blockEntity;
            upgrade = Objects.requireNonNull(upgradeTo.getEMCLinkItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getEMCLink());
            if (be.owner == null) return ActionResultType.FAIL;
            if (be.owner != player.getUUID()) {
                player.displayClientMessage(Lang.Items.MATTER_UPGRADER_NOT_OWNER.translateColored(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }

            newBlockState = upgradeBlock.defaultBlockState().setValue(TileNBTFilterable.FILTER, be.getBlockState().getValue(TileNBTFilterable.FILTER));
            TileEMCLink intBlockEntity = new TileEMCLink();
            intBlockEntity.owner = be.owner;
            intBlockEntity.ownerName = be.ownerName;
            intBlockEntity.emc = be.emc;
            intBlockEntity.itemStack = be.itemStack;
            intBlockEntity.remainingEMC = be.remainingEMC;
            intBlockEntity.remainingImport = be.remainingImport;
            intBlockEntity.remainingExport = be.remainingExport;
            intBlockEntity.remainingFluid = be.remainingFluid;
            intBlockEntity.save(new CompoundNBT());
            newBlockEntity = intBlockEntity;
        }

        if (blockEntity instanceof TileRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if (!provider.hasKnowledge(new ItemStack(upgrade)) && !player.isCreative()) {
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

        world.removeBlock(pos, false);
        world.setBlockAndUpdate(pos, newBlockState);

        if(newBlockEntity != null) {
            world.removeBlockEntity(pos);
            world.setBlockEntity(pos, newBlockEntity);
            Util.markDirty(world, pos);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(Lang.Items.MATTER_UPGRADER_DONE.translateColored(TextFormatting.WHITE, EMCFormat.format(BigInteger.valueOf(diff))), true);
        return ActionResultType.SUCCESS;
    }
}
