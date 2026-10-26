package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import cool.furry.mc.projectexpansion.gui.GUIEmcDisplayEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

// client only, opens the emc display editor
public class CommandEmcDisplay {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("emcdisplay")
            .executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                // the chat screen closes after the command runs, so the editor has to open after that
                mc.tell(() -> mc.setScreen(new GUIEmcDisplayEditor(null)));
                return Command.SINGLE_SUCCESS;
            });
    }

    // anything the client doesn't know is passed on to the server, so this can share the server's base command
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> baseNode = dispatcher.register(Commands.literal(CommandRegistry.COMMAND_BASE).then(getArguments()));
        dispatcher.register(Commands.literal("projectexpansion").redirect(baseNode));
        dispatcher.register(Commands.literal("pex").redirect(baseNode));
    }
}
