package com.alcatrazescapee.mcjunitlib.framework.mod;

import java.util.function.BiConsumer;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

import com.alcatrazescapee.mcjunitlib.framework.IntegrationTestManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

/**
 * Part of the integration test mod features.
 */
final class IntegrationTestCommands
{
    static void registerCommands(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("integrationTest")
            .then(Commands.literal("setup")
                .executes(context -> setupAllTests(context.getSource()))
            )
            .then(Commands.literal("run")
                .executes(context -> runAllTests(context.getSource()))
            )
        );
    }

    private static int runAllTests(CommandSource source)
    {
        IntegrationTestManager.INSTANCE.runAllTests(source.getLevel(), wrap(source));
        return Command.SINGLE_SUCCESS;
    }

    private static int setupAllTests(CommandSource source)
    {
        final BiConsumer<String, Boolean> logger = wrap(source);
        if (IntegrationTestManager.INSTANCE.verifyAllTests(source.getLevel(), logger))
        {
            IntegrationTestManager.INSTANCE.setupAllTests(source.getLevel(), logger);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static BiConsumer<String, Boolean> wrap(CommandSource source)
    {
        return (message, success) -> {
            final StringTextComponent text = new StringTextComponent(message);
            if (success)
            {
                source.sendSuccess(text, true);
            }
            else
            {
                source.sendFailure(text);
            }
        };
    }
}
