package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Platform;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CommandReloadEMC {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("reloademc")
            .requires(source -> Config.server.enableReloadEMCCommand.get() && Permissions.RELOAD_EMC.test(source))
            .executes(CommandReloadEMC::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        Platform.sendMessage(ctx.getSource(), Lang.Commands.RELOAD_EMC_WARNING.translateColored(ChatFormatting.RED));
        Platform.sendMessage(ctx.getSource(), Lang.Commands.RELOADING_EMC.translate());

        reload(ctx.getSource());
        return 1;
    }

    // after projecte's setemc, removeemc and resetemc, when reloadEMCAfterCommands is on
    public static void reloadAfterCommand(CommandSourceStack source, int result) {
        if (result > 0 && Config.server.reloadEMCAfterCommands.get()) reload(source);
    }

    private static void reload(CommandSourceStack source) {
        long elapsed = Util.reloadEMC(source.getServer());
        Platform.sendSuccess(source, () -> Lang.Commands.RELOAD_EMC_SUCCESS.translate(Platform.literal(String.valueOf(EMCMappingHandler.getEmcMapSize())).withStyle(ChatFormatting.GREEN), Platform.literal(elapsed + "ms").withStyle(ChatFormatting.GRAY)), true);
    }
}
