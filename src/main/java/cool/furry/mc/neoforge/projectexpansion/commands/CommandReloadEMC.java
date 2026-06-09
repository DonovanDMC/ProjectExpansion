package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class CommandReloadEMC {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("reloademc")
            .requires(source -> Config.server.enableReloadEMCCommand.get() && Permissions.RELOAD_EMC.test(source))
            .executes(CommandReloadEMC::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ctx.getSource().sendSystemMessage(Lang.Commands.RELOAD_EMC_WARNING.translateColored(ChatFormatting.RED));
        ctx.getSource().sendSystemMessage(Lang.Commands.RELOADING_EMC.translate());

        long elapsed = Util.reloadEMC(server);
        ctx.getSource().sendSuccess(() -> Lang.Commands.RELOAD_EMC_SUCCESS.translate(Component.literal(String.valueOf(EMCMappingHandler.getEmcMapSize())).withStyle(ChatFormatting.GREEN), Component.literal(elapsed + "ms").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }
}
