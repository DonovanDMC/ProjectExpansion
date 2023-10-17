package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {
    public static String COMMAND_BASE = "pex";
    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralCommandNode<CommandSourceStack> baseNode = dispatcher.register(Commands.literal(COMMAND_BASE)
                .then(CommandBook.getArguments())
                .then(CommandEMC.getArguments())
                .then(CommandKnowledge.getArguments())
        );
        dispatcher.register(Commands.literal("projectexpansion").redirect(baseNode));
    }
}
