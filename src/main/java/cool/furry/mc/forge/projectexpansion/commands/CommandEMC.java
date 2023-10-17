package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;
import java.math.BigInteger;

public class CommandEMC {

    private enum ActionType {
        ADD,
        REMOVE,
        SET,
        GET,
        TEST
    }
    public static LiteralArgumentBuilder<CommandSource> getArguments() {
        return Commands.literal("emc")
                .requires(Permissions.EMC)
                .then(Commands.literal("add")
                        .requires(Permissions.EMC_ADD)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes((ctx) -> handle(ctx, ActionType.ADD))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(Permissions.EMC_REMOVE)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes((ctx) -> handle(ctx, ActionType.REMOVE))
                                )
                        )
                )
                .then(Commands.literal("set")
                        .requires(Permissions.EMC_SET)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes((ctx) -> handle(ctx, ActionType.SET))
                                )
                        )
                )
                .then(Commands.literal("test")
                        .requires(Permissions.EMC_TEST)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes((ctx) -> handle(ctx, ActionType.TEST))
                                )
                        )
                )
                .then(Commands.literal("get")
                        .requires(Permissions.EMC_GET)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes((ctx) -> handle(ctx, ActionType.GET))
                        )
                );
    }

    private static ITextComponent getSourceName(CommandSource source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return Lang.Commands.CONSOLE.translateColored(TextFormatting.RED);
        }
    }

    private static ITextComponent formatEMC(BigInteger value) {
        return new StringTextComponent(EMCFormat.formatForceShort(value)).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(EMCFormat.formatForceLong(value))))).setStyle(ColorStyle.GRAY);
    }

    private static boolean compareUUID(CommandSource source, ServerPlayerEntity player) {
        try {
            return source.getPlayerOrException().getUUID().equals(player.getUUID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int handle(CommandContext<CommandSource> ctx, ActionType action) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        if(action == ActionType.GET) {
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            if(provider == null) {
                ctx.getSource().sendFailure(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, player.getDisplayName()));
                return 0;
            }
            if (compareUUID(ctx.getSource(), player)) {
                ctx.getSource().sendSuccess(Lang.Commands.EMC_GET_SUCCESS_SELF.translate(formatEMC(provider.getEmc())), false);
            } else {
                ctx.getSource().sendSuccess(Lang.Commands.EMC_GET_SUCCESS.translate(formatEMC(provider.getEmc())), true);
            }

            return 1;
        }
        String val = StringArgumentType.getString(ctx, "value");
        @Nullable BigInteger value = null;
        try {
            value = new BigInteger(val);
            switch (action) {
                case ADD: {
                    if (value.compareTo(BigInteger.ZERO) < 0) {
                        action = ActionType.REMOVE;
                        value = value.abs();
                    }
                    break;
                }

                case REMOVE: {
                    if (value.compareTo(BigInteger.ZERO) < 0) {
                        action = ActionType.ADD;
                        value = value.abs();
                    }
                    break;
                }

                case SET:
                case TEST: {
                    if (value.compareTo(BigInteger.ZERO) < 0) {
                        value = null;
                    }
                    break;
                }
            }
        } catch (NumberFormatException ignore) {}
        if(value == null) {
            ctx.getSource().sendFailure(Lang.Commands.EMC_INVALID.translateColored(TextFormatting.RED));
            return 0;
        }

        int response = 1;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            ctx.getSource().sendFailure(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(TextFormatting.RED, player.getDisplayName()));
            return 0;
        }
        BigInteger newEMC = provider.getEmc();
        switch (action) {
            case ADD: {
                newEMC = newEMC.add(value);
                if (compareUUID(ctx.getSource(), player)) {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_ADD_SUCCESS_SELF.translateColored(TextFormatting.GREEN, formatEMC(value), formatEMC(newEMC)), false);
                } else {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_ADD_SUCCESS.translateColored(TextFormatting.GREEN, formatEMC(value), player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        Util.sendSystemMessage(player, Lang.Commands.EMC_ADD_NOTIFICATION.translate(formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)));
                    }
                }
                break;
            }

            case REMOVE: {
                newEMC = newEMC.subtract(value);
                if (newEMC.compareTo(BigInteger.ZERO) < 0) {
                    ctx.getSource().sendFailure(Lang.Commands.EMC_REMOVE_NEGATIVE.translateColored(TextFormatting.RED, formatEMC(value), player.getScoreboardName()));
                    return 0;
                }
                if (compareUUID(ctx.getSource(), player)) {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_REMOVE_SUCCESS_SELF.translateColored(TextFormatting.GREEN, formatEMC(value), formatEMC(newEMC)), false);
                } else {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_REMOVE_SUCCESS.translateColored(TextFormatting.GREEN, formatEMC(value), player.getScoreboardName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        Util.sendSystemMessage(player, Lang.Commands.EMC_REMOVE_NOTIFICATION.translate(formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)));
                    }
                }
                break;
            }

            case SET: {
                newEMC = value;
                if (compareUUID(ctx.getSource(), player)) {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_SET_SUCCESS_SELF.translateColored(TextFormatting.GREEN, formatEMC(value), formatEMC(newEMC)), false);
                } else {
                    ctx.getSource().sendSuccess(Lang.Commands.EMC_SET_SUCCESS.translateColored(TextFormatting.GREEN, player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        Util.sendSystemMessage(player, Lang.Commands.EMC_REMOVE_SUCCESS.translate(formatEMC(newEMC), getSourceName(ctx.getSource())));
                    }
                }
                break;
            }

            case TEST: {
                boolean canTake = newEMC.compareTo(value) > -1;
                if (compareUUID(ctx.getSource(), player))
                    if (canTake) {
                        ctx.getSource().sendSuccess(Lang.Commands.EMC_TEST_SUCCESS_SELF.translateColored(TextFormatting.GREEN, formatEMC(value)), false);
                    } else {
                        response = 0;
                        ctx.getSource().sendFailure(Lang.Commands.EMC_TEST_FAIL_SELF.translateColored(TextFormatting.RED, formatEMC(value)));
                    }
                else {
                    if (canTake)
                        ctx.getSource().sendSuccess(Lang.Commands.EMC_TEST_SUCCESS.translateColored(TextFormatting.GREEN, player.getScoreboardName(), formatEMC(value)), false);
                    else {
                        response = 0;
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_TEST_FAIL_SELF.translateColored(TextFormatting.RED, player.getScoreboardName(), formatEMC(value)));
                    }
                }

                break;
            }
        }
        if(response == 1 && action != ActionType.TEST) {
            provider.setEmc(newEMC);
            provider.syncEmc(player);
        }
        return response;
    }
}