package cool.furry.mc.projectexpansion.integrations;


import cool.furry.mc.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.projectexpansion.registries.Capabilities;
import cool.furry.mc.projectexpansion.util.*;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.gameObjs.blocks.Collector;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.gameObjs.blocks.Relay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public class Common {
    public static void registerCommonTooltips(Consumer<ITextComponent> addTooltip, IDataProvider provider) {
        World level = provider.getLevel();
        BlockPos pos = provider.getBlockPos();
        Block block = provider.getBlock();
        TileEntity blockEntity = provider.getBlockEntity();
        Direction side = provider.getSide();
        BlockState state = provider.getBlockState();
        @Nullable IEmcStorage emcStorage = blockEntity == null ? null : blockEntity.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, side).resolve().orElse(null);
        @Nullable IEmcStorageBigInteger bigEmcStorage = blockEntity == null ? null : blockEntity.getCapability(Capabilities.BIG_EMC_STORAGE_CAPABILITY, side).resolve().orElse(null);
        if (bigEmcStorage != null) {
            BigInteger total = bigEmcStorage.getStoredEmcBigInteger();
            BigInteger maximum = bigEmcStorage.getMaximumEmcBigInteger();
            addTooltip.accept(Lang.EMC_STORAGE.translateColored(TextFormatting.GRAY, formatEMC(total), formatEMC(maximum)));
        } else if (emcStorage != null) { // IEmcStorageBigInteger extends IEmcStorage
            BigInteger total = BigInteger.valueOf(emcStorage.getStoredEmc());
            BigInteger maximum = BigInteger.valueOf(emcStorage.getMaximumEmc());
            addTooltip.accept(Lang.EMC_STORAGE.translateColored(TextFormatting.GRAY, formatEMC(total), formatEMC(maximum)));
        }

        if (blockEntity instanceof IGeneratesEMC) {
            IGeneratesEMC gen = ((IGeneratesEMC) blockEntity);
            BigInteger generated = gen.getGeneratedEMC();
            addTooltip.accept(Lang.EMC_PER_SECOND.translateColored(TextFormatting.GRAY, formatEMC(generated)));
        }

        if (block instanceof Collector) {
            Collector collector = ((Collector) block);
            BigInteger generated = BigInteger.valueOf(collector.getTier().getGenRate());
            addTooltip.accept(Lang.EMC_PER_SECOND.translateColored(TextFormatting.GRAY, formatEMC(generated)));
        }

        if(blockEntity instanceof IHasSunBonus && ((IHasSunBonus) blockEntity).hasSunBonus()) {
            IHasSunBonus gen = ((IHasSunBonus) blockEntity);
            int bonus = Objects.requireNonNull(gen.getSunBonus());
            addTooltip.accept(Lang.SUN_BONUS.translateColored(TextFormatting.GRAY, new StringTextComponent(String.valueOf(bonus)).withStyle(Style.EMPTY.withBold(true).withColor(TextFormatting.YELLOW))));
        }

        if (blockEntity instanceof BlockEntityEMCLink) {
            BlockEntityEMCLink link = ((BlockEntityEMCLink) blockEntity);
            Matter matter = link.getMatter();
            BigInteger emcLimit = matter.getEMCLinkEMCLimit();
            int importExportLimit = matter.getEMCLinkItemLimit();
            int fluidLimit = matter.getEMCLinkFluidLimit();
            int fluidEfficiency = matter.getFluidEfficiencyPercentage();

            addTooltip.accept(Lang.EMC_IMPORT_LIMIT.translateColored(TextFormatting.GRAY, formatEMC(link.remainingEMC), formatEMC(emcLimit)));
            addTooltip.accept(Lang.ITEM_EXPORT_LIMIT.translateColored(TextFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingExport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.ITEM_IMPORT_LIMIT.translateColored(TextFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingImport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_LIMIT.translateColored(TextFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingFluid)), formatEMC(BigInteger.valueOf(fluidLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_EFFICIENCY.translateColored(TextFormatting.GRAY, new StringTextComponent(fluidEfficiency + "%").withStyle(TextFormatting.GREEN)));
        }

        if (blockEntity instanceof BlockEntityNBTFilterable) {
            BlockEntityNBTFilterable filter = ((BlockEntityNBTFilterable) blockEntity);
            addTooltip.accept(Lang.FILTER_STATUS.translateColored(TextFormatting.GRAY, filter.getFilterStatus() ? Lang.ENABLED.translateColored(TextFormatting.GREEN) : Lang.DISABLED.translateColored(TextFormatting.RED)));
        }

        if (blockEntity instanceof IHasColor) {
            IHasColor color = ((IHasColor) blockEntity);
            addTooltip.accept(Lang.COLOR.translateColored(TextFormatting.GRAY, new StringTextComponent(Util.ucwords(color.getColor().toString())).withStyle(Style.EMPTY.withColor(Color.fromRgb(color.getColor().getTextColor())))));
        }

        if (blockEntity instanceof IHasMatter) {
            IHasMatter matter = ((IHasMatter) blockEntity);
            addTooltip.accept(Lang.MATTER.translateColored(TextFormatting.GRAY, new StringTextComponent(Util.ucwords(matter.getMatter().toString())).withStyle(Style.EMPTY.withColor(Color.fromRgb(matter.getMatter().getTextColor())))));
        } else if (blockEntity instanceof IMatterBlock) {
            IMatterBlock matter = ((IMatterBlock) blockEntity); // some will be both
            addTooltip.accept(Lang.MATTER.translateColored(TextFormatting.GRAY, new StringTextComponent(Util.ucwords(matter.getMatterType().toString())).withStyle(Style.EMPTY.withColor(TextFormatting.WHITE))));
        }

        if (blockEntity instanceof IRelayBigInteger) {
            IRelayBigInteger relay = ((IRelayBigInteger) blockEntity);
            BigInteger bonus = relay.getBonusToAddBigInteger();
            addTooltip.accept(Lang.RELAY_BONUS.translateColored(TextFormatting.GRAY, formatEMC(bonus)));
        }

        if (blockEntity instanceof BlockEntityRelay) {
            BlockEntityRelay relay = ((BlockEntityRelay) blockEntity);
            BigInteger transfer = relay.getMatter().getRelayTransfer();
            addTooltip.accept(Lang.EMC_TRANSFER_RATE.translateColored(TextFormatting.GRAY, formatEMC(transfer)));
        }

        if (block instanceof Relay) {
            Relay relay = ((Relay) block);
            long chargeRate = relay.getTier().getChargeRate();
            addTooltip.accept(Lang.CHARGE_RATE.translateColored(TextFormatting.GRAY, new StringTextComponent(String.valueOf(chargeRate))));
        }

        if (blockEntity instanceof BlockEntityOwnable) {
            BlockEntityOwnable ownable = ((BlockEntityOwnable) blockEntity);
            boolean isOwner = Objects.requireNonNull(Minecraft.getInstance().player).getUUID().equals(ownable.owner);
            String ownerName = ownable.ownerName;
            if (ownerName.isEmpty()) {
                ownerName = ownable.owner.toString();
            }
            addTooltip.accept(Lang.OWNER.translateColored(TextFormatting.GRAY, new StringTextComponent(ownerName).withStyle(isOwner ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED)));
        }
    }

    private static IFormattableTextComponent formatEMC(BigInteger value) {
        return EMCFormat.getComponent(value).withStyle(TextFormatting.GREEN);
    }

    private static IFormattableTextComponent formatEMC(BigDecimal value) {
        return EMCFormat.getComponent(value).withStyle(TextFormatting.GREEN);
    }
}
