package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {
    public static final String COMMAND_BASE = "px";
    public static final List<String> NAMES = List.of(COMMAND_BASE, "pex", "projectexpansion");
    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();

        // each name gets its own copy, pex & projectexpansion used to redirect to px, but they're sorted before it so the
        // redirect was lost from the copy of the tree sent to players, leaving them without suggestions
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .then(CommandBook.getArguments())
                    .then(CommandEMC.getArguments())
                    .then(CommandKnowledge.getArguments(buildContext))
                    .then(CommandDumpFuelMap.getArguments())
                    .then(CommandDevTest.getArguments())
                    .then(CommandWiki.getArguments())
                    .then(CommandSetOwner.getArguments())
                    .then(CommandReloadEMC.getArguments())
            );
        }
    }
}
