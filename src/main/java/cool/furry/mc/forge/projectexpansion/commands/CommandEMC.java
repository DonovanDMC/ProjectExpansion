package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

public class CommandEMC {
    private enum Action {
        ADD,
        REMOVE,
        SET
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("emc")
            .requires((source) -> source.hasPermission(2))
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

    private static Component getSourceName(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch(CommandSyntaxException e) {
            return new TranslatableComponent("command.projectexpansion.console").setStyle(ColorStyle.RED);
        }
    }

    private static UUID getSourceUUID(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getUUID();
        } catch(CommandSyntaxException e) {
            return Util.DUMMY_UUID;
        }
    }

    private static Component formatEMC(BigInteger emc) {
        return formatEMC(emc.doubleValue());
    }

    private static Component formatEMC(double emc) {
        return new TextComponent(EMCFormat.INSTANCE_IGNORE_SHIFT.format(emc)).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(String.valueOf(emc))))).setStyle(ColorStyle.GRAY);
    }

    private static boolean compareUUID(CommandSourceStack source, ServerPlayer player) {
        try {
            return source.getPlayerOrException().getUUID().equals(player.getUUID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int changeEMC(CommandContext<CommandSourceStack> ctx, Action action) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        double value = DoubleArgumentType.getDouble(ctx, "value");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        BigInteger newEMC = provider.getEmc();
        switch (action) {
            case ADD -> {
                newEMC = newEMC.add(BigDecimal.valueOf(value).toBigInteger());
                if (compareUUID(ctx.getSource(), player)) ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.add.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.add.success", formatEMC(value), player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.add.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                }
            }
            case REMOVE -> {
                newEMC = newEMC.subtract(BigDecimal.valueOf(value).toBigInteger());
                if (compareUUID(ctx.getSource(), player)) ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.remove.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.remove.success", formatEMC(value), player.getScoreboardName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.remove.notification", formatEMC(value), getSourceName(ctx.getSource()), formatEMC(newEMC)), getSourceUUID(ctx.getSource()));
                }
            }
            case SET -> {
                newEMC = BigDecimal.valueOf(value).toBigInteger();
                if (compareUUID(ctx.getSource(), player))
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.set.successSelf", formatEMC(value), formatEMC(newEMC)), false);
                else {
                    ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.set.success", player.getDisplayName(), formatEMC(newEMC)), true);
                    if (Config.notifyCommandChanges.get()) player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.set.notification", formatEMC(newEMC), getSourceName(ctx.getSource())), getSourceUUID(ctx.getSource()));
                }
            }
        }
        provider.setEmc(newEMC);
        provider.syncEmc(player);
        return 1;
    }

    private static int getEMC(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        if(compareUUID(ctx.getSource(), player)) ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.get.successSelf", formatEMC(provider.getEmc())), false);
        else ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.get.success", player.getDisplayName(), formatEMC(provider.getEmc())), true);
        return 1;
    }

    private static int clearKnowledge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        provider.clearKnowledge();
        provider.sync(player);
        if (compareUUID(ctx.getSource(), player))
            ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.clearKnowledge.successSelf"), false);
        else {
            ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.clearKnowledge.success", player.getDisplayName()), true);
            player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.clearKnowledge.notification", getSourceName(ctx.getSource())), getSourceUUID(ctx.getSource()));
        }
        return 1;
    }

    private static int learn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Item item = ItemArgument.getItem(ctx, "item").getItem();

        IKnowledgeProvider provider = ProjectEAPI.getTransmutationProxy().getKnowledgeProviderFor(player.getUUID());
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        if (!proxy.hasValue(item)) {
            ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.learn.invalidItem"));
            return 0;
        }
        boolean isSelf = compareUUID(ctx.getSource(), player);
        if (!provider.addKnowledge(ItemInfo.fromItem(item))) {
            if (isSelf)
                ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.learn.failSelf", new ItemStack(item).getDisplayName()).setStyle(ColorStyle.RED));
            else
                ctx.getSource().sendFailure(new TranslatableComponent("command.projectexpansion.emc.learn.fail", player.getDisplayName(), new ItemStack(item).getDisplayName()).setStyle(ColorStyle.RED));
            return 0;
        }
        provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromItem(item)), true);
        if (isSelf)
            ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.learn.successSelf", new ItemStack(item).getDisplayName()).setStyle(ColorStyle.GREEN), false);
        else {
            ctx.getSource().sendSuccess(new TranslatableComponent("command.projectexpansion.emc.learn.success", player.getDisplayName(), new ItemStack(item).getDisplayName()).setStyle(ColorStyle.GRAY), true);
            if (Config.notifyCommandChanges.get())
                player.sendMessage(new TranslatableComponent("command.projectexpansion.emc.learn.notification", new ItemStack(item).getDisplayName(), getSourceName(ctx.getSource())).setStyle(ColorStyle.GRAY), getSourceUUID(ctx.getSource()));
        }
        return 1;
    }
}
