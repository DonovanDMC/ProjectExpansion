package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class CommandRegistry {
    public static String COMMAND_BASE = "pex";
    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();

        LiteralCommandNode<CommandSourceStack> baseNode = dispatcher.register(Commands.literal(COMMAND_BASE)
                .then(CommandBook.getArguments())
                .then(CommandEMC.getArguments())
                .then(CommandKnowledge.getArguments(buildContext))
                .then(CommandDumpFuelMap.getArguments())
                .then(CommandDevTest.getArguments())
                .then(CommandWiki.getArguments())
        );
        dispatcher.register(Commands.literal("projectexpansion").redirect(baseNode));
    }
}
