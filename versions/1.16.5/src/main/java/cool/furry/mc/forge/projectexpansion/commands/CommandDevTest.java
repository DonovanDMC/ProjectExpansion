package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class CommandDevTest {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("devtest").requires(ignore -> !FMLEnvironment.production)
                .executes(CommandDevTest::handle);
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    public static int handle(CommandContext<CommandSource> ctx) {
        return 1;
    }
}
