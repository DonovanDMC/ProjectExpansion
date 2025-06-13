package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.TableGenerator;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.FuelMapper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommandDumpFuelMap {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("dumpfuelmap")
                .requires(Permissions.DUMP_FUEL_MAP)
                .executes(CommandDumpFuelMap::handle);
    }

    private static int handle(CommandContext<CommandSourceStack> ctx) {
        HolderSet<Item> items = FuelMapper.getFuelMap();
        TableGenerator table = new TableGenerator(TableGenerator.Alignment.CENTER, TableGenerator.Alignment.CENTER, TableGenerator.Alignment.CENTER);
        TableGenerator.Receiver receiver = ctx.getSource().getPlayer() == null ? TableGenerator.Receiver.CONSOLE : TableGenerator.Receiver.CLIENT;
        table.addRow(Lang.Commands.DUMP_FUEL_MAP_FUEL.translate(), Lang.Commands.DUMP_FUEL_MAP_FUEL.translate(), Lang.Commands.DUMP_FUEL_MAP_INDEX.translate());

        BigDecimal previous = BigDecimal.ZERO;
        IEMCProxy proxy = IEMCProxy.INSTANCE;
        for (int index = 0; index < items.size(); index++) {
            Holder<Item> item = items.get(index);
            ItemStack stack = new ItemStack(item);
            BigDecimal emc = BigDecimal.valueOf(proxy.getValue(item));
            BigDecimal diff = emc.subtract(previous);
            String percent = diff.divide(emc, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).toPlainString();
            table.addRow(stack.getDisplayName(), Component.literal(EMCFormat.wrap(emc).forceShort().ignoreShift().format()).append(Component.literal(" (").withStyle(ChatFormatting.WHITE)).append("+").append(EMCFormat.wrap(diff).forceShort().ignoreShift().format()).append(", ").append(Component.literal(percent.substring(0, percent.length() - 3)).append("%").withStyle(ChatFormatting.RED)).append(Component.literal(")").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.YELLOW), Component.literal(String.valueOf(index)).withStyle(ChatFormatting.RED));
            previous = emc;
        }

        table.eachRow(receiver, components -> { ctx.getSource().sendSystemMessage(components.get(0).copy().append(" | ").append(components.get(1)).append(" | ").append(components.get(2))); });
        return 1;
    }
}