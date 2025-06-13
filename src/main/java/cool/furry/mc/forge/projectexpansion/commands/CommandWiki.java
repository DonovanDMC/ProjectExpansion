package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class CommandWiki {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("wiki")
                .requires(Permissions.WIKI)
                .executes(CommandWiki::handle);
    }

    private static int handle(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal(Util.WIKI).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Util.WIKI))));
        return 1;
    }
}