package cool.furry.mc.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class CommandDevTest {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("devtest").requires(ignore -> !FMLEnvironment.production)
                .executes(CommandDevTest::handle);
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    public static int handle(CommandContext<CommandSourceStack> ctx) {
        return 1;
    }
}
