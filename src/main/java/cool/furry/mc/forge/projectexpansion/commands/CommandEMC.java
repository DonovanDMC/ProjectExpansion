package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

public class CommandEMC {

    private enum Action {
        ADD,
        REMOVE,
        SET,
        GET,
        TEST
    }
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> cmd = Commands.literal("emc")
            .requires((source) -> source.hasPermissionLevel(2))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handleEMC(ctx, Action.ADD))
                    )
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handleEMC(ctx, Action.REMOVE))
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes((ctx) -> handleEMC(ctx, Action.SET))
                    )
                )
            )
            .then(Commands.literal("test")
                  .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", StringArgumentType.string())
                              .executes((ctx) -> handleEMC(ctx, Action.TEST))
                        )
                  )
            )
            .then(Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.player())
                              .executes((ctx) -> handleEMC(ctx, Action.GET))
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
            return new TranslationTextComponent("command.projectexpansion.console").mergeStyle(TextFormatting.RED);
        }
    }

    private static UUID getSourceUUID(CommandSource source) {
        try {
            return source.asPlayer().getUniqueID();
        } catch (CommandSyntaxException e) {
            return Util.DUMMY_UUID;
        }
    }

    private static ITextComponent formatEMC(BigInteger value) {
        return new StringTextComponent(EMCFormat.formatForceShort(value)).setStyle(Style.EMPTY.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(EMCFormat.formatForceLong(value))))).mergeStyle(TextFormatting.GRAY);
    }

    private static boolean compareUUID(CommandSource source, ServerPlayerEntity player) {
        try {
            return source.asPlayer().getUniqueID().equals(player.getUniqueID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int handleEMC(CommandContext<CommandSource> ctx, Action action) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        if(action == Action.GET) {
            IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
            if (compareUUID(ctx.getSource(), player))
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.get.successSelf", formatEMC(provider.getEmc())), false);
            else
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.get.success", player.getDisplayName(), formatEMC(provider.getEmc())), true);

            return 1;
        }
        String val = StringArgumentType.getString(ctx, "value");
        @Nullable BigInteger value = null;
        try {
            value = new BigInteger(val);
            switch(action) {
                case ADD: {
                    if(value.compareTo(BigInteger.ZERO) < 0) {
                        action = Action.REMOVE;
                        value = value.abs();
                    }
                    break;
                }

                case REMOVE: {
                    if(value.compareTo(BigInteger.ZERO) < 0) {
                        action = Action.ADD;
                        value = value.abs();
                    }
                    break;
                }

                case SET:
                case TEST: {
                    if(value.compareTo(BigInteger.ZERO) < 0) value = null;
                    break;
                }
            }
        } catch (NumberFormatException ignore) {}
        if(value == null) {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.invalid_value", val).mergeStyle(TextFormatting.RED), false);
            return 0;
        }

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUniqueID());
        BigInteger newEMC = provider.getEmc();
        switch (action) {
            case ADD: {
                newEMC = newEMC.add(value);
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.add.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.add.success", formatEMC(value), player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.add.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                    }
                }
                break;
            }

            case REMOVE: {
                newEMC = newEMC.subtract(value);
                if(newEMC.compareTo(BigInteger.ZERO) < 0) {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.remove.negative", formatEMC(value), player.getScoreboardName()), false);
                    return 0;
                }
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.remove.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.remove.success", formatEMC(value), player.getScoreboardName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.remove.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                    }
                }
                break;
            }

            case SET: {
                newEMC = value;
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.set.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.set.success", player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) {
                        player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.set.notification", formatEMC(newEMC), getSourceName(ctx.getSource())), getSourceUUID(ctx.getSource()));
                    }
                }
                break;
            }

            case TEST: {
                boolean canTake = newEMC.compareTo(value) > -1;
                if (compareUUID(ctx.getSource(), player))
                    if(canTake) ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.test.successSelf", formatEMC(value)), false);
                    else ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.test.failSelf", formatEMC(value)), false);
                else {
                    if(canTake) ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.test.success", player.getScoreboardName(), formatEMC(value)), false);
                    else ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.test.fail", player.getScoreboardName(), formatEMC(value)), false);
                }
            }
        }
        provider.setEmc(newEMC);
        provider.syncEmc(player);
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
            player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.clearKnowledge.notification", getSourceName(ctx.getSource())), getSourceUUID(ctx.getSource()));
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
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.failSelf", new ItemStack(item).getTextComponent()).mergeStyle(TextFormatting.RED), false);
             else
                ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.fail", player.getDisplayName(), new ItemStack(item).getTextComponent()).mergeStyle(TextFormatting.RED), true);

            return 0;
        }
        provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromItem(item)), true);
        if (isSelf) {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.successSelf", new ItemStack(item).getTextComponent()).mergeStyle(TextFormatting.GREEN), false);
        } else {
            ctx.getSource().sendFeedback(new TranslationTextComponent("command.projectexpansion.emc.learn.success", player.getDisplayName(), new ItemStack(item).getTextComponent()).mergeStyle(TextFormatting.GRAY), true);
            if (Config.notifyCommandChanges.get()) {
                player.sendMessage(new TranslationTextComponent("command.projectexpansion.emc.learn.notification", new ItemStack(item).getTextComponent(), getSourceName(ctx.getSource())).mergeStyle(TextFormatting.GRAY), getSourceUUID(ctx.getSource()));
            }
        }

        return 1;
    }
}
