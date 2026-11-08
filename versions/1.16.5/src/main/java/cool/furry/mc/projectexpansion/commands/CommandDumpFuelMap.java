package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.util.EMCFormat;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.TableGenerator;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.FuelMapper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CommandDumpFuelMap {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("dumpfuelmap")
                .requires(Permissions.DUMP_FUEL_MAP)
                .executes(CommandDumpFuelMap::handle);
    }

    @SuppressWarnings("SameReturnValue")
    private static int handle(CommandContext<CommandSource> ctx) {
        List<Item> items = FuelMapper.getFuelMap();
        TableGenerator table = new TableGenerator(TableGenerator.Alignment.CENTER, TableGenerator.Alignment.CENTER, TableGenerator.Alignment.CENTER);
        TableGenerator.Receiver receiver = Util.getPlayer(ctx.getSource()) == null ? TableGenerator.Receiver.CONSOLE : TableGenerator.Receiver.CLIENT;
        table.addRow(Lang.Commands.DUMP_FUEL_MAP_FUEL.translate(), Lang.Commands.DUMP_FUEL_MAP_EMC.translate(), Lang.Commands.DUMP_FUEL_MAP_INDEX.translate());

        BigDecimal previous = BigDecimal.ZERO;
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            ItemStack stack = new ItemStack(item);
            BigDecimal emc = BigDecimal.valueOf(proxy.getValue(item));
            BigDecimal diff = emc.subtract(previous);
            String percent = diff.divide(emc, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).toPlainString();
            table.addRow(stack.getDisplayName(), new StringTextComponent(EMCFormat.wrap(emc).forceShort().ignoreShift().format()).append(new StringTextComponent(" (").withStyle(TextFormatting.WHITE)).append("+").append(EMCFormat.wrap(diff).forceShort().ignoreShift().format()).append(", ").append(new StringTextComponent(percent.substring(0, percent.length() - 3)).append("%").withStyle(TextFormatting.RED)).append(new StringTextComponent(")").withStyle(TextFormatting.WHITE)).withStyle(TextFormatting.YELLOW), new StringTextComponent(String.valueOf(index)).withStyle(TextFormatting.RED));
            previous = emc;
        }

        table.eachRow(receiver, components -> ctx.getSource().sendSuccess(components.get(0).copy().append(" | ").append(components.get(1)).append(" | ").append(components.get(2)), false));
        return 1;
    }
}