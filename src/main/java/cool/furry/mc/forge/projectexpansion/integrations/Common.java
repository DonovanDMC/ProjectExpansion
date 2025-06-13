package cool.furry.mc.neoforge.projectexpansion.integrations;


import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.neoforge.projectexpansion.registries.Capabilities;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.api.block_entity.IRelay;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.gameObjs.blocks.Collector;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.gameObjs.blocks.Relay;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public class Common {
    public static void registerCommonTooltips(Consumer<Component> addTooltip, IDataProvider provider) {
        Level level = provider.getLevel();
        BlockPos pos = provider.getBlockPos();
        Block block = provider.getBlock();
        BlockEntity blockEntity = provider.getBlockEntity();
        Direction side = provider.getSide();
        BlockState state = provider.getBlockState();
        @Nullable IEmcStorage emcStorage = WorldHelper.getCapability(level, PECapabilities.EMC_STORAGE_CAPABILITY, pos, state, blockEntity, side);
        @Nullable IEmcStorageBigInteger bigEmcStorage = WorldHelper.getCapability(level, Capabilities.BIG_EMC_STORAGE_CAPABILITY, pos, state, blockEntity, side);
        if (bigEmcStorage != null) {
            BigInteger total = bigEmcStorage.getStoredEmcBigInteger();
            BigInteger maximum = bigEmcStorage.getMaximumEmcBigInteger();
            addTooltip.accept(Lang.EMC_STORAGE.translateColored(ChatFormatting.GRAY, formatEMC(total), formatEMC(maximum)));
        } else if (emcStorage != null) { // IEmcStorageBigInteger extends IEmcStorage
            BigInteger total = BigInteger.valueOf(emcStorage.getStoredEmc());
            BigInteger maximum = BigInteger.valueOf(emcStorage.getMaximumEmc());
            addTooltip.accept(Lang.EMC_STORAGE.translateColored(ChatFormatting.GRAY, formatEMC(total), formatEMC(maximum)));
        }

        if (blockEntity instanceof IGeneratesEMC gen) {
            BigInteger generated = gen.getGeneratedEMC();
            addTooltip.accept(Lang.EMC_PER_SECOND.translateColored(ChatFormatting.GRAY, formatEMC(generated)));
        }

        if (block instanceof Collector collector) {
            BigInteger generated = BigInteger.valueOf(collector.getTier().getGenRate());
            addTooltip.accept(Lang.EMC_PER_SECOND.translateColored(ChatFormatting.GRAY, formatEMC(generated)));
        }

        if(blockEntity instanceof IHasSunBonus gen && gen.hasSunBonus()) {
            int bonus = Objects.requireNonNull(gen.getSunBonus());
            addTooltip.accept(Lang.SUN_BONUS.translateColored(ChatFormatting.GRAY, Component.literal(String.valueOf(bonus)).withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.YELLOW))));
        }

        if (blockEntity instanceof BlockEntityEMCLink link) {
            Matter matter = link.getMatter();
            BigInteger emcLimit = matter.getEMCLinkEMCLimit();
            int importExportLimit = matter.getEMCLinkItemLimit();
            int fluidLimit = matter.getEMCLinkFluidLimit();
            int fluidEfficiency = matter.getFluidEfficiencyPercentage();

            addTooltip.accept(Lang.EMC_IMPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(link.remainingEMC), formatEMC(emcLimit)));
            addTooltip.accept(Lang.ITEM_EXPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingExport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.ITEM_IMPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingImport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingFluid)), formatEMC(BigInteger.valueOf(fluidLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_EFFICIENCY.translateColored(ChatFormatting.GRAY, Component.literal(fluidEfficiency + "%").withStyle(ChatFormatting.GREEN)));
        }

        if (blockEntity instanceof BlockEntityNBTFilterable filter) {
            addTooltip.accept(Lang.FILTER_STATUS.translateColored(ChatFormatting.GRAY, filter.getFilterStatus() ? Lang.ENABLED.translateColored(ChatFormatting.GREEN) : Lang.DISABLED.translateColored(ChatFormatting.RED)));
        }

        if (blockEntity instanceof IHasColor color) {
            addTooltip.accept(Lang.COLOR.translateColored(ChatFormatting.GRAY, Component.literal(Util.ucwords(color.getColor().toString())).withStyle(Style.EMPTY.withColor(color.getColor().getTextColor()))));
        }

        if (blockEntity instanceof IHasMatter matter) {
            addTooltip.accept(Lang.MATTER.translateColored(ChatFormatting.GRAY, Component.literal(Util.ucwords(matter.getMatter().toString())).withStyle(Style.EMPTY.withColor(matter.getMatter().getTextColor()))));
        } else if (blockEntity instanceof IMatterBlock matter) { // some will be both
            addTooltip.accept(Lang.MATTER.translateColored(ChatFormatting.GRAY, Component.literal(Util.ucwords(matter.getMatterType().toString())).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))));
        }

        if (blockEntity instanceof IRelayBigInteger relay) {
            BigInteger bonus = relay.getBonusToAddBigInteger();
            addTooltip.accept(Lang.RELAY_BONUS.translateColored(ChatFormatting.GRAY, formatEMC(bonus)));
        } else if (blockEntity instanceof IRelay relay) { // IRelayBigInteger extends IRelay
            BigDecimal bonus = BigDecimal.valueOf(relay.getBonusToAdd());
            addTooltip.accept(Lang.RELAY_BONUS.translateColored(ChatFormatting.GRAY, formatEMC(bonus)));
        }

        if (blockEntity instanceof BlockEntityRelay relay) {
            BigInteger transfer = relay.getMatter().getRelayTransfer();
            addTooltip.accept(Lang.EMC_TRANSFER_RATE.translateColored(ChatFormatting.GRAY, formatEMC(transfer)));
        }

        if (block instanceof Relay relay) {
            long chargeRate = relay.getTier().getChargeRate();
            addTooltip.accept(Lang.CHARGE_RATE.translateColored(ChatFormatting.GRAY, Component.literal(String.valueOf(chargeRate))));
        }

        if (blockEntity instanceof BlockEntityOwnable ownable) {
            boolean isOwner = Objects.requireNonNull(Minecraft.getInstance().player).getUUID().equals(ownable.owner);
            String ownerName = ownable.ownerName;
            if (ownerName.isEmpty()) {
                ownerName = ownable.owner.toString();
            }
            addTooltip.accept(Lang.OWNER.translateColored(ChatFormatting.GRAY, Component.literal(ownerName).withStyle(isOwner ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
        }

        addTooltip.accept(Component.literal(String.valueOf(new Random().nextInt())));
    }

    private static MutableComponent formatEMC(BigInteger value) {
        return EMCFormat.getComponent(value).withStyle(ChatFormatting.GREEN);
    }

    private static MutableComponent formatEMC(BigDecimal value) {
        return EMCFormat.getComponent(value).withStyle(ChatFormatting.GREEN);
    }
}
