package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.tile.TileCollector;
import cool.furry.mc.forge.projectexpansion.tile.TilePowerFlower;
import cool.furry.mc.forge.projectexpansion.tile.TileRelay;
import cool.furry.mc.forge.projectexpansion.util.HasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.UUID;

public class ItemUpgrade extends Item implements HasMatter {
    public enum UpgradeType {
        COLLECTOR,
        POWER_FLOWER,
        RELAY;

        public String getName() {
            switch(this) {
                case COLLECTOR: return "Collector";
                case POWER_FLOWER: return "Power Flower";
                case RELAY: return "Relay";
            }

            return "Unknown";
        }
    }
    private final Matter matter;
    private final UpgradeType type;
    public ItemUpgrade(Matter matter, UpgradeType type) {
        super(new Item.Properties().group(Main.group));
        this.matter = matter;
        this.type = type;
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    public UpgradeType getType() {
        return type;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.addInformation(stack, world, list, flag);
        list.add(new TranslationTextComponent("text.projectexpansion.upgrade_tooltip", type.getName()).mergeStyle(TextFormatting.GRAY));
        list.add(new TranslationTextComponent("text.projectexpansion.upgrade_wip").mergeStyle(TextFormatting.RED));
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        Main.Logger.info(String.format("onItemUseFirst %s %s %s %s", this, stack, getMatter(), getType()));
        @Nullable PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getPos();
        World world = context.getWorld();
        ItemStack itemStack = context.getItem();
        ItemUpgrade upgrade = (ItemUpgrade) itemStack.getItem();

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
            player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.max_upgrade").mergeStyle(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }
        @Nullable UUID owner = null;
        @Nullable String ownerName = null;
        @Nullable BigInteger emc = null;
        if(tile instanceof TilePowerFlower) {
            TilePowerFlower tilePowerFlower = (TilePowerFlower) tile;
            owner = tilePowerFlower.owner;
            ownerName = tilePowerFlower.ownerName;
            emc = tilePowerFlower.emc;
            if(owner == null) return ActionResultType.FAIL;
            if(owner != player.getUniqueID()) {
                    player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.upgrade_not_owner").mergeStyle(TextFormatting.RED), true);
                    return ActionResultType.FAIL;
                }
        }

        if(tile instanceof TileCollector) {
            if(upgrade.type != UpgradeType.COLLECTOR) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            if(itemStack.getItem() != upgradeTo.getCollectorUpgrade()) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade_tier").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            world.removeBlock(pos, false);
            world.setBlockState(pos, Objects.requireNonNull(upgradeTo.getCollector()).getDefaultState());
        }
        else if(tile instanceof TilePowerFlower && ownerName != null && emc != null) {
            if(upgrade.type != UpgradeType.POWER_FLOWER) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            if(itemStack.getItem() != upgradeTo.getPowerFlowerUpgrade()) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade_tier").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            world.removeBlock(pos, false);
            world.setBlockState(pos, Objects.requireNonNull(upgradeTo.getPowerFlower()).getDefaultState());
            TilePowerFlower newTile = new TilePowerFlower();
            newTile.owner = owner;
            newTile.ownerName = ownerName;
            newTile.emc = emc;
            newTile.markDirty();
            world.removeTileEntity(pos);
            world.setTileEntity(pos, newTile);
        }
        else if(tile instanceof TileRelay) {
            if(upgrade.type != UpgradeType.RELAY) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            if(itemStack.getItem() != upgradeTo.getRelayUpgrade()) {
                player.sendStatusMessage(new TranslationTextComponent("text.projectexpansion.incorrect_upgrade_tier").mergeStyle(TextFormatting.RED), true);
                return ActionResultType.FAIL;
            }
            world.removeBlock(pos, false);
            world.setBlockState(pos, Objects.requireNonNull(upgradeTo.getRelay()).getDefaultState());
        }
        else return ActionResultType.FAIL;

        if(!player.abilities.isCreativeMode) itemStack.shrink(1);
        return ActionResultType.SUCCESS;
    }
}
