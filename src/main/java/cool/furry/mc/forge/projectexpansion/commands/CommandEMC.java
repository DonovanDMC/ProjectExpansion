package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class CommandEMC {

    private enum ActionType {
        ADD,
        REMOVE,
        SET,
        GET,
        TEST
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("emc")
            .requires((source) -> source.hasPermission(2))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handle(ctx, ActionType.ADD))
                    )
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handle(ctx, ActionType.REMOVE))
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handle(ctx, ActionType.SET))
                    )
               )
            )
            .then(Commands.literal("test")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handle(ctx, ActionType.TEST))
                   )
                )
            )
            .then(Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes((ctx) -> handle(ctx, ActionType.GET))
                )
            );

        dispatcher.register(cmd);
    }

    private static Component getSourceName(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return new TranslatableComponent("command.projectexpansion.console").setStyle(ColorStyle.RED);
        }
    }

    private static UUID getSourceUUID(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getUUID();
        } catch (CommandSyntaxException e) {
            return Util.DUMMY_UUID;
        }
    }

    private static Component formatEMC(BigInteger value) {
        return new TextComponent(EMCFormat.formatForceShort(value)).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(EMCFormat.formatForceLong(value))))).setStyle(ColorStyle.GRAY);
    }

    private static boolean compareUUID(CommandSourceStack source, ServerPlayer player) {
        try {
            return source.getPlayerOrException().getUUID().equals(player.getUUID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int handle(CommandContext<CommandSourceStack> ctx, ActionType action) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if(action == ActionType.GET) {
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            if(provider == null) {
                ctx.getSource().sendFailure(new TranslatableComponent("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED));
                return 0;
            }
            if (compareUUID(ctx.getSource(), player)) ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.get.successSelf", formatEMC(provider.getEmc())), false);
            else ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.get.success", player.getDisplayName(), formatEMC(provider.getEmc())), true);

            return 1;
        }
        String val = StringArgumentType.getString(ctx, "value");
        @Nullable BigInteger value = null;
        try {
            value = new BigInteger(val);
            switch (action) {
                case ADD -> {
                    if (value.compareTo(BigInteger.ZERO) < 0) {
                        action = ActionType.REMOVE;
                        value = value.abs();
                    }
                }
                case REMOVE -> {
                    if (value.compareTo(BigInteger.ZERO) < 0) {
                        action = ActionType.ADD;
                        value = value.abs();
                    }
                }
                case SET, TEST -> {
                    if (value.compareTo(BigInteger.ZERO) < 0) value = null;
                }
            }
        } catch (NumberFormatException ignore) {}
        if(value == null) {
            ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.invalid", val).setStyle(ColorStyle.RED));
            return 0;
        }

        int response = 1;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            ctx.getSource().sendFailure(new TranslatableComponent("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED));
            return 0;
        }
        BigInteger newEMC = provider.getEmc();
        switch (action) {
            case ADD -> {
                newEMC = newEMC.add(value);
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.add.successSelf", formatEMC(value), formatEMC(newEMC)).setStyle(ColorStyle.GREEN), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.add.success", formatEMC(value), player.getDisplayName(), formatEMC(newEMC)).setStyle(ColorStyle.GREEN), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.add.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                }
            }
            case REMOVE -> {
                newEMC = newEMC.subtract(value);
                if (newEMC.compareTo(BigInteger.ZERO) < 0) {
                    ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.remove.negative", formatEMC(value), player.getScoreboardName()).setStyle(ColorStyle.RED));
                    return 0;
                }
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.remove.successSelf", formatEMC(value), formatEMC(newEMC)).setStyle(ColorStyle.GREEN), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.remove.success", formatEMC(value), player.getScoreboardName(), formatEMC(newEMC)).setStyle(ColorStyle.GREEN), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.remove.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                }
            }
            case SET -> {
                newEMC = value;
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.set.successSelf", formatEMC(newEMC)).setStyle(ColorStyle.GREEN), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.set.success", player.getDisplayName(), formatEMC(newEMC)).setStyle(ColorStyle.GREEN), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.set.notification", formatEMC(newEMC), getSourceName(ctx.getSource())), getSourceUUID(ctx.getSource()));
                }
            }
            case TEST -> {
                boolean canTake = newEMC.compareTo(value) > -1;
                if (compareUUID(ctx.getSource(), player))
                    if (canTake) {
                        ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.test.successSelf", formatEMC(value)).setStyle(ColorStyle.GREEN), false);
                    } else {
                        response = 0;
                        ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.test.failSelf", formatEMC(value)).setStyle(ColorStyle.RED));
                    }
                else {
                    if (canTake)
                        ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.test.success", player.getScoreboardName(), formatEMC(value)).setStyle(ColorStyle.GREEN), false);
                    else {
                        response = 0;
                        ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.test.fail", player.getScoreboardName(), formatEMC(value)).setStyle(ColorStyle.RED));
                    }
                }
            }
        }
        if(response == 1 && action != ActionType.TEST) {
            provider.setEmc(newEMC);
            provider.syncEmc(player);
        }
        return 1;
    }
}