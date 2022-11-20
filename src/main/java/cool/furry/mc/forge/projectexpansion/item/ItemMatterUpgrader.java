package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.TileCollector;
import cool.furry.mc.forge.projectexpansion.tile.TileEMCLink;
import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemMatterUpgrader extends Item {
    public ItemMatterUpgrader() {
        super(new Item.Properties().tab(Main.tab));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip").setStyle(ColorStyle.GRAY));
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip2").setStyle(ColorStyle.GREEN));
        list.add(new TranslationTextComponent("item.projectexpansion.matter_upgrader.tooltip_creative").setStyle(ColorStyle.RED));
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
            player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.max_upgrade").setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            player.displayClientMessage(new TranslationTextComponent("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();

        @Nullable BlockItem upgrade = null;
        @Nullable Block upgradeBlock = null;
        @Nullable UUID owner = null;
        @Nullable String ownerName = null;
        @Nullable BigInteger emc = null;

        if (blockEntity instanceof TileCollector) {
            upgrade = Objects.requireNonNull(upgradeTo.getCollectorItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getCollector());
        }

        if (blockEntity instanceof TilePowerFlower) {
            TilePowerFlower bePowerFlower = (TilePowerFlower) blockEntity;
            upgrade = Objects.requireNonNull(upgradeTo.getPowerFlowerItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getPowerFlower());
            owner = bePowerFlower.owner;
            ownerName = bePowerFlower.ownerName;
            emc = bePowerFlower.emc;
            if (owner == null) return ActionResultType.FAIL;
            if (owner != player.getUUID()) {
                player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_owner").setStyle(ColorStyle.RED), true);
                return ActionResultType.FAIL;
            }
        }

        if (blockEntity instanceof TileRelay) {
            upgrade = Objects.requireNonNull(upgradeTo.getRelayItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getRelay());
        }

        if (blockEntity instanceof TileEMCLink) {
            upgrade = Objects.requireNonNull(upgradeTo.getEMCLinkItem());
            upgradeBlock = Objects.requireNonNull(upgradeTo.getEMCLink());
        }

        if (!provider.hasKnowledge(new ItemStack(upgrade)) && !player.isCreative()) {
            player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_learned", new TranslationTextComponent(Objects.requireNonNull(upgrade).toString())).setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        long prevValue = proxy.getValue(block);
        long emcValue = proxy.getValue(Objects.requireNonNull(upgrade));
        long diff = emcValue - prevValue;
        if (player.isCreative()) diff = 0;
        BigInteger newEmc = provider.getEmc().subtract(BigInteger.valueOf(diff));
        if (newEmc.compareTo(BigInteger.ZERO) < 0) {
            player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.not_enough_emc", EMCFormat.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.RED), true);
            return ActionResultType.FAIL;
        }

        world.removeBlock(pos, false);
        world.setBlockAndUpdate(pos, upgradeBlock.defaultBlockState());

        if (blockEntity instanceof TilePowerFlower) {
            if (ownerName == null || emc == null) return ActionResultType.FAIL;

            TilePowerFlower newTile = new TilePowerFlower();
            newTile.owner = owner;
            newTile.ownerName = ownerName;
            newTile.emc = emc;
            blockEntity.save(new CompoundNBT());
            Util.markDirty(blockEntity);
            world.removeBlockEntity(pos);
            world.setBlockEntity(pos, newTile);
        }

        provider.setEmc(newEmc);
        player.displayClientMessage(new TranslationTextComponent("item.projectexpansion.matter_upgrader.done", EMCFormat.format(BigInteger.valueOf(diff))).setStyle(ColorStyle.WHITE), true);
        return ActionResultType.SUCCESS;
    }
}
