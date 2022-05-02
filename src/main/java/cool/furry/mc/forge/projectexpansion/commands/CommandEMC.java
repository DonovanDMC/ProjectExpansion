package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.HoverEvent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

public class CommandEMC {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> cmd = Commands.literal("emc")
                .requires((source) -> source.hasPermissionLevel(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1))
                                        .executes((ctx) -> changeEMC(ctx, Action.ADD))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1))
                                        .executes((ctx) -> changeEMC(ctx, Action.REMOVE))
                                )
                        )
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                        .executes((ctx) -> changeEMC(ctx, Action.SET))
                                )
                        )
                )
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CommandEMC::getEMC)
                        )
                )
                .then(Commands.literal("clearKnowledge")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CommandEMC::clearKnowledge)
                        )
                )
                .then(Commands.literal("learn")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("item", ItemArgument.item())
                                        .executes(CommandEMC::learn)
                                )
                        )
                );

        dispatcher.register(cmd);
    }

    private static ITextComponent getSourceName(CommandSource source) {
        try {
            return source.asPlayer().getDisplayName();
        } catch (CommandSyntaxException e) {
            return new TranslationTextComponent("command.projectexpansion.console").setStyle(ColorStyle.RED);
        }
    }


    @SuppressWarnings("unused")
    private static UUID getSourceUUID(CommandSource source) {
        try {
            return source.asPlayer().getUniqueID();
        } catch (CommandSyntaxException e) {
            return new UUID(0L, 0L);
        }
    }

    private static ITextComponent formatEMC(BigInteger emc) {
        return formatEMC(emc.doubleValue());
    }

    private static ITextComponent formatEMC(double emc) {
        return new StringTextComponent(EMCFormat.INSTANCE_IGNORE_SHIFT.format(emc)).setStyle(ColorStyle.GRAY.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(String.valueOf(emc)))));
    }

    private static boolean compareUUID(CommandSource source, ServerPlayerEntity player) {
        try {
            return source.asPlayer().getUniqueID().equals(player.getUniqueID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int changeEMC(CommandContext<CommandSource> ctx, Action action) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        double value = DoubleArgumentType.getDouble(ctx, "value");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        BigInteger newEMC = provider.getEmc();
        switch (action) {
            case ADD: {
                newEMC = newEMC.add(BigDecimal.valueOf(value).toBigInteger());
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.add.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.add.success", formatEMC(value), player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.add.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), ChatType.CHAT);
                }
                break;
            }

            case REMOVE: {
                newEMC = newEMC.subtract(BigDecimal.valueOf(value).toBigInteger());
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.remove.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.remove.success", formatEMC(value), player.getScoreboardName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.remove.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), ChatType.CHAT);
                }
                break;
            }

            case SET: {
                newEMC = BigDecimal.valueOf(value).toBigInteger();
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.set.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.set.success", player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get())
                        player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.set.notification", formatEMC(newEMC), getSourceName(ctx.getSource())), ChatType.CHAT);
                }
                break;
            }
        }
        provider.setEmc(newEMC);
        provider.sync(player);
        return 1;
    }

    private static int getEMC(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        if (compareUUID(ctx.getSource(), player))
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.get.successSelf", formatEMC(provider.getEmc())), false);
        else
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.get.success", player.getDisplayName(), formatEMC(provider.getEmc())), true);
        return 1;
    }

    private static int clearKnowledge(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        provider.clearKnowledge();
        provider.sync(player);
        if (compareUUID(ctx.getSource(), player))
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.clearKnowledge.successSelf"), false);
        else {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.clearKnowledge.success", player.getDisplayName()), true);
            player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.clearKnowledge.notification", getSourceName(ctx.getSource())), ChatType.CHAT);
        }
        return 1;
    }

    private static int learn(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        Item item = ItemArgument.getItem(ctx, "item").getItem();

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        if (!proxy.hasValue(item)) {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.invalidItem"), false);
            return 0;
        }
        boolean isSelf = compareUUID(ctx.getSource(), player);
        if (!provider.addKnowledge(ItemInfo.fromItem(item))) {
            if (isSelf)
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.failSelf", new ItemStack(item).getTextComponent()).setStyle(ColorStyle.RED), false);
            else
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.fail", player.getDisplayName(), new ItemStack(item).getTextComponent()).setStyle(ColorStyle.RED), true);
            return 0;
        }
        provider.sync(player);
        if (isSelf)
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.successSelf", new ItemStack(item).getTextComponent()).setStyle(ColorStyle.GREEN), false);
        else {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.success", player.getDisplayName(), new ItemStack(item).getTextComponent()).setStyle(ColorStyle.GRAY), true);
            if (Config.notifyCommandChanges.get())
                player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.learn.notification", new ItemStack(item).getTextComponent(), getSourceName(ctx.getSource())).setStyle(ColorStyle.GRAY), ChatType.CHAT);
        }
        return 1;
    }

    private enum Action {
        ADD,
        REMOVE,
        SET
    }
}