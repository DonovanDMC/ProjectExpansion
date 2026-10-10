package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.furry.mc.projectexpansion.util.Util;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

public class CommandWiki {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("wiki")
                .requires(Permissions.WIKI)
                .executes(CommandWiki::handle);
    }

    @SuppressWarnings("SameReturnValue")
    private static int handle(CommandContext<CommandSource> ctx) {
        ctx.getSource().sendSuccess(new StringTextComponent(Util.WIKI).withStyle(Style.EMPTY.withColor(TextFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Util.WIKI))), false);
        return 1;
    }
}