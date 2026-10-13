package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.util.Lang;
import cool.furry.mc.projectexpansion.util.Util;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class CommandReloadEMC {
    // lets tests turn reloading on without changing the config, as forge can read the saved file back half written
    public static boolean forceReloadAfterCommands = false;

    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("reloademc")
            .requires(source -> Config.server.enableReloadEMCCommand.get() && Permissions.RELOAD_EMC.test(source))
            .executes(CommandReloadEMC::execute);
    }

    private static int execute(CommandContext<CommandSource> ctx) {
        ctx.getSource().sendSuccess(Lang.Commands.RELOAD_EMC_WARNING.translateColored(TextFormatting.RED), false);
        ctx.getSource().sendSuccess(Lang.Commands.RELOADING_EMC.translate(), false);

        reload(ctx.getSource());
        return 1;
    }

    public static boolean reloadsAfterCommands() {
        return forceReloadAfterCommands || Config.server.reloadEMCAfterCommands.get();
    }

    // after projecte's setemc, removeemc and resetemc, when reloadEMCAfterCommands is on
    public static void reloadAfterCommand(CommandSource source, int result) {
        if (result > 0 && reloadsAfterCommands()) reload(source);
    }

    private static void reload(CommandSource source) {
        long elapsed = Util.reloadEMC(source.getServer());
        source.sendSuccess(Lang.Commands.RELOAD_EMC_SUCCESS.translate(new StringTextComponent(String.valueOf(EMCMappingHandler.getEmcMapSize())).withStyle(TextFormatting.GREEN), new StringTextComponent(elapsed + "ms").withStyle(TextFormatting.GRAY)), true);
    }
}
