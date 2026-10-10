package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {
    public static final String COMMAND_BASE = "px";
    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        LiteralCommandNode<CommandSource> baseNode = dispatcher.register(Commands.literal(COMMAND_BASE)
                .then(CommandBook.getArguments())
                .then(CommandEMC.getArguments())
                .then(CommandKnowledge.getArguments())
                .then(CommandDumpFuelMap.getArguments())
                .then(CommandDevTest.getArguments())
                .then(CommandWiki.getArguments())
                .then(CommandSetOwner.getArguments())
                .then(CommandReloadEMC.getArguments())
        );
        dispatcher.register(Commands.literal("projectexpansion").redirect(baseNode));
        dispatcher.register(Commands.literal("pex").redirect(baseNode));
    }
}
