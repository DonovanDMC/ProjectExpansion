package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.TileCollector;
import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.HasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.block.Block;
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
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemUpgrade extends Item {
    public ItemUpgrade() {
        super(new Item.Properties().group(Main.group));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, world, list, flag);
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip2").setStyle(ColorStyle.GREEN));
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip_creative").setStyle(ColorStyle.RED));
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        @Nullable PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getPos();
        World world = context.getWorld();
        ItemStack itemStack = context.getItem();

        if (world.isRemote || player == null) return ActionResultType.PASS;

        TileEntity tile = world.getTileEntity(pos);
        Block block = world.getBlockState(pos).getBlock();

        Matter matter;
        Matter upgradeTo;
        if(block instanceof HasMatter) {
            matter = ((HasMatter) block).getMatter();
            upgradeTo = matter.next();
        }
        else return ActionResultType.PASS;

        if (matter == Matter.FINAL) {
            player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.max_upgrade").setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        @Nullable IKnowledgeProvider provider = player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).orElse(null);
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        if(provider == null) {
            player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.provider_error").setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable UUID owner = null;
        @Nullable String ownerName = null;
        @Nullable BigInteger emc = null;

        if(tile instanceof TileCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if(tile instanceof TilePowerFlower) {
            TilePowerFlower tilePowerFlower = (TilePowerFlower) tile;
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            owner = tilePowerFlower.owner;
            ownerName = tilePowerFlower.ownerName;
            emc = tilePowerFlower.emc;
            if(owner == null) return ActionResultType.FAIL;
            if(owner != player.getUniqueID()) {
                player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_owner").setStyle(ColorStyle.RED), true);
                return ActionResultType.FAIL;
            }
        }

        if(tile instanceof TileRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if(!provider.hasKnowledge(new ItemStack(upgrade)) && !player.abilities.isCreativeMode) {
            player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_learned", new TranslationTextComponent(upgrade.getTranslationKey())).setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if(player.abilities.isCreativeMode) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if(newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_enough_emc", EMCFormat.INSTANCE.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        world.removeBlock(pos, false);
        world.setBlockState(pos, upgradeBlock.getDefaultState());

        if(tile instanceof TilePowerFlower) {
            if(owner == null || ownerName == null || emc == null) {
                return ActionResultType.FAIL;
            }

            TilePowerFlower newTile = new TilePowerFlower();
            newTile.owner = owner;
            newTile.ownerName = ownerName;
            newTile.emc = emc;
            newTile.markDirty();
            world.removeTileEntity(pos);
            world.setTileEntity(pos, newTile);
        }

        provider.setEmc(newEmc);
        player.sendStatusMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.done", EMCFormat.INSTANCE.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.WHITE), true);
        return ActionResultType.SUCCESS;
    }
}