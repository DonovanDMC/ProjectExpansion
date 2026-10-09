package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommandSetOwner {
    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("setowner").requires(Permissions.SET_OWNER)
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(CommandSetOwner::handle)
                    )
                );
    }

    public static int handle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockEntity blockEntity = WorldHelper.getBlockEntity(ctx.getSource().getLevel(), pos);
        if (blockEntity instanceof BlockEntityOwnable be) {
            be.setOwner(player);
            ctx.getSource().sendSystemMessage(Lang.Commands.SET_OWNER_SUCCESS.translateColored(ChatFormatting.GREEN, player.getDisplayName()));
            return 1;
        } else {
            ctx.getSource().sendSystemMessage(Lang.Commands.SET_OWNER_FAILURE.translateColored(ChatFormatting.RED));
            return 0;
        }
    }
}
