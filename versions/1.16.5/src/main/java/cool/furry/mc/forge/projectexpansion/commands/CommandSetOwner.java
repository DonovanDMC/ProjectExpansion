package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class CommandSetOwner {
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("setowner").requires(Permissions.SET_OWNER)
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(CommandSetOwner::handle)
                    )
                );
    }

    public static int handle(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        TileEntity blockEntity = WorldHelper.getTileEntity(ctx.getSource().getLevel(), pos);
        if (blockEntity instanceof BlockEntityOwnable) {
            BlockEntityOwnable be = ((BlockEntityOwnable) blockEntity);
            be.setOwner(player);
            ctx.getSource().sendSuccess(Lang.Commands.SET_OWNER_SUCCESS.translateColored(TextFormatting.GREEN, player.getDisplayName()), false);
            return 1;
        } else {
            ctx.getSource().sendSuccess(Lang.Commands.SET_OWNER_FAILURE.translateColored(TextFormatting.RED), false);
            return 0;
        }
    }
}
